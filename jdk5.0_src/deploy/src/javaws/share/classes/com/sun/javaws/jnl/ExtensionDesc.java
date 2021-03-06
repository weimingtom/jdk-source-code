/*
 * %W% %E%
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.javaws.jnl;
import java.net.URL;
import java.util.HashSet;
import com.sun.deploy.xml.*;

/**
 *  class that describes an extension. An extension is a
 *  library that might contain native code.
 */
public class ExtensionDesc implements ResourceType {
    // Tag elements
    private String         _name;
    private URL            _location;
    private String         _version;
    private boolean        _isInstaller;
    
    // List of ext-download elements
    private ExtDownloadDesc[] _extDownloadDescs;
    
    // Link to launchDesc
    private LaunchDesc     _extensionDefDesc; // Link to launchDesc for extension
    
    public ExtensionDesc(String name, URL location, String version, ExtDownloadDesc[] extDownloadDescs) {
        _location = location;
        _version = version;
        _name = name;
        if (extDownloadDescs == null) extDownloadDescs = new ExtDownloadDesc[0];
        _extDownloadDescs = extDownloadDescs;
        _extensionDefDesc = null;
	_isInstaller = false;
        
    }
    /*
     *   Information about the extension
     */
    public void   setInstaller(boolean isInstaller) { _isInstaller = isInstaller; }
    public boolean isInstaller() { return _isInstaller; }
    public String getVersion()  { return _version; }
    public URL    getLocation() { return _location; }
    public String getName()     { return _name; }
    ExtDownloadDesc[] getExtDownloadDescs() { return _extDownloadDescs; }
    
    /*
     * Information about the resources
     */
    public LaunchDesc getExtensionDesc() { return _extensionDefDesc; }
    public void setExtensionDesc(LaunchDesc desc) { _extensionDefDesc = desc;; }
    
    ResourcesDesc getExtensionResources() { 
	return _extensionDefDesc.getResources(); 
    }
    
    /** Returns the set of parts that are required in the extension 
     *  based on the 'eager' flag and the set of parts that are 
     *  needed in the current JNLP file
     */
    HashSet getExtensionPackages(HashSet parts, boolean includeEager) {
        HashSet map = new HashSet();
        for(int i = 0; i < _extDownloadDescs.length; i++) {
	    ExtDownloadDesc edd = _extDownloadDescs[i];
	    // An entry is included if and only if:
	    //  - download="eager" and includeEager is set, or
	    //  - part is included in 'parts',
	    boolean isEager = includeEager && !edd.isLazy();
	    if (isEager || (parts != null && parts.contains(edd.getPart()))) {
		map.add(edd.getExtensionPart());
	    }
        }
        return map;
    }
    
    /** Visitor dispatch */
    public void visit(ResourceVisitor rv) {
        rv.visitExtensionDesc(this);
    }
    
    /** Return contents as an XML document */
    public XMLNode asXML() {
        XMLAttributeBuilder ab = new XMLAttributeBuilder();
        ab.add("href", _location);
        ab.add("version", _version);
        ab.add("name", _name);
        XMLNodeBuilder nb = new XMLNodeBuilder("extension", ab.getAttributeList());
	for(int i = 0; i < _extDownloadDescs.length; i++) {
	    nb.add(_extDownloadDescs[i]);
	}
        return nb.getNode();
    }
}

