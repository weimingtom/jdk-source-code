/*
 * @(#)ReliableLog.java	1.17 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.rmi.log;

import java.io.*;

/**
 * This class is a simple implementation of a reliable Log.  The
 * client of a ReliableLog must provide a set of callbacks (via a
 * LogHandler) that enables a ReliableLog to read and write
 * checkpoints and log records.  This implementation ensures that the
 * current value of the data stored (via a ReliableLog) is recoverable
 * after a system crash. <p>
 *
 * The secondary storage strategy is to record values in files using a
 * representation of the caller's choosing.  Two sorts of files are
 * kept: snapshots and logs.  At any instant, one snapshot is current.
 * The log consists of a sequence of updates that have occurred since
 * the current snapshot was taken.  The current stable state is the
 * value of the snapshot, as modified by the sequence of updates in
 * the log.  From time to time, the client of a ReliableLog instructs
 * the package to make a new snapshot and clear the log.  A ReliableLog
 * arranges disk writes such that updates are stable (as long as the
 * changes are force-written to disk) and atomic : no update is lost,
 * and each update either is recorded completely in the log or not at
 * all.  Making a new snapshot is also atomic. <p>
 *
 * Normal use for maintaining the recoverable store is as follows: The
 * client maintains the relevant data structure in virtual memory.  As
 * updates happen to the structure, the client informs the ReliableLog
 * (all it "log") by calling log.update.  Periodically, the client
 * calls log.snapshot to provide the current value of the data
 * structure.  On restart, the client calls log.recover to obtain the
 * latest snapshot and the following sequences of updates; the client
 * applies the updates to the snapshot to obtain the state that
 * existed before the crash. <p>
 *
 * The current logfile format is: <ol>
 * <li> a format version number (two 4-octet integers, major and
 * minor), followed by
 * <li> a sequence of log records.  Each log record contains, in
 * order, <ol>
 * <li> a 4-octet integer representing the length of the following log
 * data,
 * <li> the log data (variable length). </ol> </ol> <p>
 *
 * @see LogHandler
 *
 * @author Ann Wollrath
 *
 */
public class ReliableLog {
 
    public final static int PreferredMajorVersion = 0;
    public final static int PreferredMinorVersion = 1;

    // sun.rmi.log.debug=false
    private boolean Debug = false;
    
    private static String snapshotPrefix = "Snapshot.";
    private static String logfilePrefix = "Logfile.";
    private static String versionFile = "Version_Number";
    private static String newVersionFile = "New_Version_Number";
    private static int	  intBytes = 4;
    private static long   diskPageSize = 512;

    private File dir;			// base directory
    private int version = 0;		// current snapshot and log version
    private String logName = null;
    private RandomAccessFile log = null;
    private FileDescriptor logFD;
    private long snapshotBytes = 0;
    private long logBytes = 0;
    private int logEntries = 0;
    private long lastSnapshot = 0;
    private long lastLog = 0;
    //private long padBoundary = intBytes;
    private LogHandler handler;
    private final byte[] intBuf = new byte[4];

    // format version numbers read from/written to this.log
    private int majorFormatVersion = 0;
    private int minorFormatVersion = 0;

    /**
     * Creates a ReliableLog to handle checkpoints and logging in a
     * stable storage directory.
     *
     * @param dirPath path to the stable storage directory
     * @param logCl the closure object containing callbacks for logging and
     * recovery
     * @param pad ignored
     * @exception IOException If a directory creation error has
     * occurred or if initialSnapshot callback raises an exception or
     * if an exception occurs during invocation of the handler's
     * snapshot method or if other IOException occurs.
     */
    public ReliableLog(String dirPath,
		     LogHandler handler,
		     boolean pad)
	throws IOException
    {
	super();
	this.Debug = ((Boolean)java.security.AccessController.doPrivileged
	    (new sun.security.action.GetBooleanAction
	    ("sun.rmi.log.debug"))).booleanValue();
	dir = new File(dirPath);
	if (!(dir.exists() && dir.isDirectory())) {
	    // create directory
	    if (!dir.mkdir()) {
		throw new IOException("could not create directory for log: " +
				      dirPath);
	    }
	}
	//padBoundary = (pad ? diskPageSize : intBytes);
	this.handler = handler;
	lastSnapshot = 0;
	lastLog = 0;
	getVersion();
	if (version == 0) {
	    try {
		snapshot(handler.initialSnapshot());
	    } catch (IOException e) {
		throw e;
	    } catch (Exception e) {
		throw new IOException("initial snapshot failed with " +
				      "exception: " + e);
	    }
	}
    }

    /**
     * Creates a ReliableLog to handle checkpoints and logging in a
     * stable storage directory.
     *
     * @param dirPath path to the stable storage directory
     * @param logCl the closure object containing callbacks for logging and
     * recovery
     * @exception IOException If a directory creation error has
     * occurred or if initialSnapshot callback raises an exception
     */
    public ReliableLog(String dirPath,
		     LogHandler handler)
	throws IOException
    {
	this(dirPath, handler, false);
    }
    
    /* public methods */
    
    /**
     * Returns an object which is the value recorded in the current
     * snapshot.  This snapshot is recovered by calling the client
     * supplied callback "recover" and then subsequently invoking
     * the "readUpdate" callback to apply any logged updates to the state.
     *
     * @exception IOException If recovery fails due to serious log
     * corruption, read update failure, or if an exception occurs
     * during the recover callback
     */
    public synchronized Object recover()
	throws IOException
    {
	if (Debug)
	    System.err.println("log.debug: recover()");

	if (version == 0)
	    return null;
	
	Object snapshot;
	String fname = versionName(snapshotPrefix);
	File snapshotFile = new File(fname);
	InputStream in =
	        new BufferedInputStream(new FileInputStream(snapshotFile));

	if (Debug)
	    System.err.println("log.debug: recovering from " + fname);
	
	try {
	    try {
		snapshot = handler.recover(in);
		
	    } catch (IOException e) {
		throw e;
	    } catch (Exception e) {
		if (Debug)
		    System.err.println("log.debug: recovery failed: " + e);
		throw new IOException("log recover failed with " +
				      "exception: " + e);
	    }
	    snapshotBytes = snapshotFile.length();
	} finally {
	    in.close();
	}
	
	return recoverUpdates(snapshot);
    }
    
    /**
     * Records this update in the log file (does not force update to disk).
     * The update is recorded by calling the client's "writeUpdate" callback.
     * This method must not be called until this log's recover method has
     * been invoked (and completed).
     *
     * @param value the object representing the update
     * @exception IOException If an exception occurred during a
     * writeUpdate callback or if other I/O error has occurred.
     */
    public synchronized void update(Object value) throws IOException {
	update(value, false);
    }
    
    /**
     * Records this update in the log file.  The update is recorded by
     * calling the client's writeUpdate callback.  This method must not be
     * called until this log's recover method has been invoked
     * (and completed).
     *
     * @param value the object representing the update
     * @param forceToDisk If true, changes are forced to disk, otherwise
     * updates are buffered.
     * @exception IOException If force-write to log failed or an
     * exception occurred during the writeUpdate callback or if other
     * I/O error occurs while updating the log.
     */
    public synchronized void update(Object value, boolean forceToDisk)
	throws IOException
    {
	// avoid accessing a null log field.
	if (log == null) {
	    throw new IOException("rmid's log is inaccessible, " +
	        "it may have been corrupted or closed");
	}

	long entryStart = log.getFilePointer();
	writeInt(log, 0);
	logFD.sync();
	
	try {
	    handler.writeUpdate(new LogOutputStream(log), value);
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    throw new IOException("write update failed with exception: " +
				  e.getClass().getName() +
				  ", message: " + e.getMessage());
	}
	long entryEnd = log.getFilePointer();
	long updateLen = (entryEnd - entryStart) - intBytes;
	    
	logFD.sync();
	
	/* write update record length before update in file */
	log.seek(entryStart);
	writeInt(log, (int) updateLen);
	log.seek(entryEnd);

	logBytes = entryEnd;

	if (forceToDisk) {
	    logFD.sync();
	}
	
	lastLog = System.currentTimeMillis();
	logEntries++;
    }
    
    /**
     * Records this value as the current snapshot by invoking the client
     * supplied "snapshot" callback and then empties the log.
     *
     * @param value the object representing the new snapshot
     * @exception IOException If an exception occurred during the
     * snapshot callback or if other I/O error has occurred during the
     * snapshot process
     */
    public synchronized void snapshot(Object value)
	throws IOException
    {
	int oldVersion = version;
	incrVersion();

	String fname = versionName(snapshotPrefix);
	File snapshotFile = new File(fname);
	FileOutputStream out = new FileOutputStream(snapshotFile);
	try {
	    try {
		handler.snapshot(out, value);
	    } catch (IOException e) {
		throw e;
	    } catch (Exception e) {
		throw new IOException("snapshot failed with exception of type: " +
				      e.getClass().getName() +
				      ", message was: " + e.getMessage());
	    }
	    lastSnapshot = System.currentTimeMillis();
	} finally {
	    out.close();
	    snapshotBytes = snapshotFile.length();
	}

	openLogFile(true);
	writeVersionFile(true);
	commitToNewVersion();
	deleteSnapshot(oldVersion);
	deleteLogFile(oldVersion);
    }
    
    /**
     * Close the stable storage directory in an orderly manner.
     *
     * @exception IOException If an I/O error occurs when the log is
     * closed
     */
    public synchronized void close() throws IOException {
	if (log == null) return;
	try {
	    log.close();
	} finally {
	    log = null;
	}
    }
    
    /**
     * Returns the size of the snapshot file in bytes;
     */
    public long snapshotSize() {
	return snapshotBytes;
    }
    
    /**
     * Returns the size of the log file in bytes;
     */
    public long logSize() {
	return logBytes;
    }

    /* private methods */

    /**
     * Write an int value in single write operation.  This method
     * assumes that the caller is synchronized on the log file.
     *
     * @param out output stream
     * @param val int value
     * @throws IOException if any other I/O error occurs
     */
    private void writeInt(DataOutput out, int val) throws IOException {
	intBuf[0] = (byte) (val >> 24);
	intBuf[1] = (byte) (val >> 16);
	intBuf[2] = (byte) (val >> 8);
	intBuf[3] = (byte) val;
	out.write(intBuf);
    }

    /**
     * Generates a filename prepended with the stable storage directory path.
     *
     * @param name the leaf name of the file
     */
    private String fName(String name) {
	return dir.getPath() + File.separator + name;
    }

    /**
     * Generates a version 0 filename prepended with the stable storage
     * directory path
     *
     * @param name version file name
     */
    private String versionName(String name) {
	return versionName(name, 0);
    }
    
    /**
     * Generates a version filename prepended with the stable storage
     * directory path with the version number as a suffix.
     *
     * @param name version file name
     * @thisversion a version number
     */
    private String versionName(String prefix, int ver) {
	ver = (ver == 0) ? version : ver;
	return fName(prefix) + String.valueOf(ver);
    }

    /**
     * Increments the directory version number.
     */
    private void incrVersion() {
	do { version++; } while (version==0);
    }

    /**
     * Delete a file.
     *
     * @param name the name of the file
     * @exception IOException If new version file couldn't be removed
     */
    private void deleteFile(String name) throws IOException {

	File f = new File(name);
	if (!f.delete())
	    throw new IOException("couldn't remove file: " + name); 
    }
    
    /**
     * Removes the new version number file.
     *
     * @exception IOException If an I/O error has occurred.
     */
    private void deleteNewVersionFile() throws IOException {
	deleteFile(fName(newVersionFile));
    }

    /**
     * Removes the snapshot file.
     *
     * @param ver the version to remove
     * @exception IOException If an I/O error has occurred.
     */
    private void deleteSnapshot(int ver) throws IOException {
	if (ver == 0) return;
	deleteFile(versionName(snapshotPrefix, ver));
    }

    /**
     * Removes the log file.
     *
     * @param ver the version to remove
     * @exception IOException If an I/O error has occurred.
     */
    private void deleteLogFile(int ver) throws IOException {
	if (ver == 0) return;
	deleteFile(versionName(logfilePrefix, ver));
    }

    /**
     * Opens the log file in read/write mode.  If file does not exist, it is
     * created.
     *
     * @param truncate if true and file exists, file is truncated to zero
     * length
     * @exception IOException If an I/O error has occurred.
     */
    private void openLogFile(boolean truncate) throws IOException {
	try {
	    close();
	} catch (IOException e) { /* assume this is okay */
	}
	
	logName = versionName(logfilePrefix);
	log = new RandomAccessFile(logName, "rw");
	logFD = log.getFD();

	if (truncate) {
	    initializeLogFile();
	}
    }
    
    /**
     * Creates a new log file, truncated and initialized with the format
     * version number preferred by this implementation.
     * <p>Environment: inited, synchronized
     * <p>Precondition: valid: log, log contains nothing useful
     * <p>Postcondition: if successful, log is initialised with the format
     * version number (Preferred{Major,Minor}Version), and logBytes is
     * set to the resulting size of the updatelog, and logEntries is set to
     * zero.  Otherwise, log is in an indeterminate state, and logBytes
     * is unchanged, and logEntries is unchanged.
     *
     * @exception IOException If an I/O error has occurred.
     */
    private void initializeLogFile()
	throws IOException
    {
	log.setLength(0);
	majorFormatVersion = PreferredMajorVersion;
	writeInt(log, PreferredMajorVersion);
	minorFormatVersion = PreferredMinorVersion;
	writeInt(log, PreferredMinorVersion);
	logBytes = intBytes * 2;
	logEntries = 0;
    }
    
    
    /**
     * Writes out version number to file.
     *
     * @param newVersion if true, writes to a new version file
     * @exception IOException If an I/O error has occurred.
     */
    private void writeVersionFile(boolean newVersion) throws IOException {
	String name;
	if (newVersion) {
	    name = newVersionFile;
	} else {
	    name = versionFile;
	}
	DataOutputStream out =
	    new DataOutputStream(new FileOutputStream(fName(name)));
	writeInt(out, version);
	out.close();
    }
    
    /**
     * Creates the initial version file
     *
     * @exception IOException If an I/O error has occurred.
     */
    private void createFirstVersion() throws IOException {
	version = 0;
	writeVersionFile(false);
    }
    
    /**
     * Commits (atomically) the new version.
     *
     * @exception IOException If an I/O error has occurred.
     */
    private void commitToNewVersion() throws IOException {
	writeVersionFile(false);
	deleteNewVersionFile();
    }

    /**
     * Reads version number from a file.
     *
     * @param name the name of the version file
     * @return the version
     * @exception IOException If an I/O error has occurred.
     */
    private int readVersion(String name) throws IOException {
	DataInputStream in = new DataInputStream(new FileInputStream(name));
	try {
	    return in.readInt();
	} finally {
	    in.close();
	}
    }
    
    /**
     * Sets the version.  If version file does not exist, the initial
     * version file is created.
     *
     * @exception IOException If an I/O error has occurred.
     */
    private void getVersion() throws IOException {
	try {
	    version = readVersion(fName(newVersionFile));
	    commitToNewVersion();
	} catch (IOException e) {
	    try {
		deleteNewVersionFile();
	    }
	    catch (IOException ex) {
	    }
	    
	    try {
		version = readVersion(fName(versionFile));
	    }
	    catch (IOException ex) {
		createFirstVersion();
	    }
	}
    }

    /**
     * Applies outstanding updates to the snapshot.
     *
     * @param state the most recent snapshot
     * @exception IOException If serious log corruption is detected or
     * if an exception occurred during a readUpdate callback or if
     * other I/O error has occurred.
     * @return the resulting state of the object after all updates
     */
    private Object recoverUpdates(Object state)
	throws IOException
    {
	logBytes = 0;
	logEntries = 0;

	if (version == 0) return state;
	
	String fname = versionName(logfilePrefix);
        InputStream in =
	        new BufferedInputStream(new FileInputStream(fname));
	DataInputStream dataIn = new DataInputStream(in);

	if (Debug)
	    System.err.println("log.debug: reading updates from " + fname);
	
	try {
	    majorFormatVersion = dataIn.readInt(); logBytes += intBytes;
	    minorFormatVersion = dataIn.readInt(); logBytes += intBytes;
	} catch (EOFException e) {
	    /* This is a log which was corrupted and/or cleared (by
	     * fsck or equivalent).  This is not an error.
	     */
	    openLogFile(true);	// create and truncate
	    in = null;
	}
	/* A new major version number is a catastrophe (it means
	 * that the file format is incompatible with older
	 * clients, and we'll only be breaking things by trying to
	 * use the log).  A new minor version is no big deal for
	 * upward compatibility.
	 */
	if (majorFormatVersion != PreferredMajorVersion) {
	    if (Debug) {
		System.err.println("log.debug: major version mismatch: " +
			majorFormatVersion + "." + minorFormatVersion);
	    }
	    throw new IOException("Log file " + logName + " has a " +
				  "version " + majorFormatVersion +
				  "." + minorFormatVersion +
				  " format, and this implementation " +
				  " understands only version " +
				  PreferredMajorVersion + "." +
				  PreferredMinorVersion);
	}
	
	try {
	    while (in != null) {
		int updateLen = 0;
		
		try {
		    updateLen = dataIn.readInt();
		} catch (EOFException e) {
		    if (Debug)
			System.err.println("log.debug: log was sync'd cleanly");
		    break;
		}
		if (updateLen == 0) {/* crashed while writing last log entry */
		    if (Debug)
			System.err.println("log.debug: last update incomplete");
		    break;
		}
		if (updateLen < 0) {  /* serious corruption */
		    if (Debug)
			System.err.println("log.debug: length = " + updateLen);
		    throw new IOException("corrupted log: bad update length");
		}

		// this is a fragile use of available() which relies on the
		// twin facts that BufferedInputStream correctly consults
		// the underlying stream, and that FileInputStream returns
		// the number of bytes remaining in the file (via FIONREAD).
		if (in.available() < updateLen) {
		    /* corrupted record at end of log (can happen since we
		     * do only one fsync)
		     */
		    if (Debug)
			System.err.println("log.debug: log was truncated");
		    break;
		}

		if (Debug)
		    System.err.println("log.debug: rdUpdate size " + updateLen);
		try {
		    state = handler.readUpdate(new LogInputStream(in, updateLen),
					  state);
		} catch (IOException e) {
		    throw e;
		} catch (Exception e) {
		    e.printStackTrace();
		    throw new IOException("read update failed with " +
					  "exception: " + e);
		}
		logBytes += (intBytes + updateLen);
		logEntries++;
	    } /* while */
	} finally {
	    if (in != null)
		in.close();
	}

	if (Debug)
	    System.err.println("log.debug: recovered updates: " + logEntries);

	/* reopen log file at end */
	openLogFile(false);

	// avoid accessing a null log field
	if (log == null) {
	    throw new IOException("rmid's log is inaccessible, " +
	        "it may have been corrupted or closed");
	}

	log.seek(logBytes);
	log.setLength(logBytes);

	return state;
    }

//     /**
//      * Returns the number of bytes to pads the log file: to either the
//      * disk page boundary or next word boundary.
//      * @param pos the file position from which to start padding
//      * @return bytes of padding
//      */
//     private int getPadding(long pos) {
// 	/* this avoids a method call to abs */
// 	int padding = (int)(padBoundary - (pos % padBoundary));
// 	return (padding != padBoundary) ? padding : 0;
//     }
}