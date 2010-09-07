/*
 * @(#)OIDName.java	1.13 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.x509;

import java.io.IOException;

import sun.security.util.*;

/**
 * This class implements the OIDName as required by the GeneralNames
 * ASN.1 object.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @version 1.13
 * @see GeneralName
 * @see GeneralNames
 * @see GeneralNameInterface
 */
public class OIDName implements GeneralNameInterface {
     private ObjectIdentifier oid;

    /**
     * Create the OIDName object from the passed encoded Der value.
     *
     * @param derValue the encoded DER OIDName.
     * @exception IOException on error.
     */
    public OIDName(DerValue derValue) throws IOException {
        oid = derValue.getOID();
    }

    /**
     * Create the OIDName object with the specified name.
     *
     * @param name the OIDName.
     */
    public OIDName(ObjectIdentifier oid) {
        this.oid = oid;
    }

    /**
     * Create the OIDName from the String form of the OID
     *
     * @param name the OIDName in form "x.y.z..."
     * @throws IOException on error
     */
    public OIDName(String name) throws IOException {
	try {
	    oid = new ObjectIdentifier(name);
	} catch (Exception e) {
	    throw new IOException("Unable to create OIDName: " + e);
	}
    }

    /**
     * Return the type of the GeneralName.
     */
    public int getType() {
        return (GeneralNameInterface.NAME_OID);
    }

    /**
     * Encode the OID name into the DerOutputStream.
     *
     * @param out the DER stream to encode the OIDName to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        out.putOID(oid);
    }

    /**
     * Convert the name into user readable string.
     */
    public String toString() {
        return ("OIDName: " + oid.toString());
    }

    /**
     * Returns this OID name.
     */
    public ObjectIdentifier getOID() {
        return oid;
    }

    /**
     * Compares this name with another, for equality.
     *
     * @return true iff the names are identical
     */
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;

	if (!(obj instanceof OIDName))
	    return false;

	OIDName other = (OIDName)obj;
	
	return oid.equals(other.oid);
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode() {
	return oid.hashCode();
    }

    /**
     * Return type of constraint inputName places on this name:<ul>
     *   <li>NAME_DIFF_TYPE = -1: input name is different type from name (i.e. does not constrain).
     *   <li>NAME_MATCH = 0: input name matches name.
     *   <li>NAME_NARROWS = 1: input name narrows name (is lower in the naming subtree)
     *   <li>NAME_WIDENS = 2: input name widens name (is higher in the naming subtree)
     *   <li>NAME_SAME_TYPE = 3: input name does not match or narrow name, but is same type.
     * </ul>.  These results are used in checking NameConstraints during
     * certification path verification.
     *
     * @param inputName to be checked for being constrained
     * @returns constraint type above
     * @throws UnsupportedOperationException if name is not exact match, but narrowing and widening are
     *          not supported for this name type.
     */
    public int constrains(GeneralNameInterface inputName) throws UnsupportedOperationException {
	int constraintType;
	if (inputName == null)
	    constraintType = NAME_DIFF_TYPE;
	else if (inputName.getType() != NAME_OID)
	    constraintType = NAME_DIFF_TYPE;
	else if (this.equals((OIDName)inputName))
	    constraintType = NAME_MATCH;
	else
	    //widens and narrows not defined in RFC2459 for OIDName (aka registeredID)
	    throw new UnsupportedOperationException("Narrowing and widening are not supported for OIDNames");
	return constraintType;
    }

    /**
     * Return subtree depth of this name for purposes of determining
     * NameConstraints minimum and maximum bounds and for calculating
     * path lengths in name subtrees.
     *
     * @returns distance of name from root
     * @throws UnsupportedOperationException if not supported for this name type
     */
    public int subtreeDepth() throws UnsupportedOperationException {
	throw new UnsupportedOperationException("subtreeDepth() not supported for OIDName.");
   }
}