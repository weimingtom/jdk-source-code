/*
 * @(#)PolicyConstraintsExtension.java	1.26 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Vector;

import sun.security.util.*;

/**
 * This class defines the certificate extension which specifies the
 * Policy constraints.
 * <p>
 * The policy constraints extension can be used in certificates issued
 * to CAs. The policy constraints extension constrains path validation
 * in two ways. It can be used to prohibit policy mapping or require
 * that each certificate in a path contain an acceptable policy
 * identifier.<p>
 * The ASN.1 syntax for this is (IMPLICIT tagging is defined in the
 * module definition):
 * <pre>
 * PolicyConstraints ::= SEQUENCE {
 *     requireExplicitPolicy [0] SkipCerts OPTIONAL,
 *     inhibitPolicyMapping  [1] SkipCerts OPTIONAL
 * }
 * SkipCerts ::= INTEGER (0..MAX)
 * </pre>
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @version 1.9
 * @see Extension
 * @see CertAttrSet
 */
public class PolicyConstraintsExtension extends Extension
implements CertAttrSet {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions.PolicyConstraints";
    /**
     * Attribute names.
     */
    public static final String NAME = "PolicyConstraints";
    public static final String REQUIRE = "require";
    public static final String INHIBIT = "inhibit";

    private static final byte TAG_REQUIRE = 0;
    private static final byte TAG_INHIBIT = 1;

    private int require = -1;
    private int inhibit = -1;

    // Encode this extension value.
    private void encodeThis() throws IOException {
        if (require == -1 && inhibit == -1) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream tagged = new DerOutputStream();
        DerOutputStream seq = new DerOutputStream();

        if (require != -1) {
            DerOutputStream tmp = new DerOutputStream();
            tmp.putInteger(require);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT,
                         false, TAG_REQUIRE), tmp);
        }
        if (inhibit != -1) {
            DerOutputStream tmp = new DerOutputStream();
            tmp.putInteger(inhibit);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT,
                         false, TAG_INHIBIT), tmp);
        }
        seq.write(DerValue.tag_Sequence, tagged);
	this.extensionValue = seq.toByteArray();
    }

    /**
     * Create a PolicyConstraintsExtension object with both
     * require explicit policy and inhibit policy mapping. The
     * extension is marked non-critical.
     *
     * @param require require explicit policy (-1 for optional).
     * @param inhibit inhibit policy mapping (-1 for optional).
     */
    public PolicyConstraintsExtension(int require, int inhibit)
    throws IOException {
	this(Boolean.FALSE, require, inhibit);
    }

    /**
     * Create a PolicyConstraintsExtension object with specified
     * criticality and both require explicit policy and inhibit 
     * policy mapping.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param require require explicit policy (-1 for optional).
     * @param inhibit inhibit policy mapping (-1 for optional).
     */
    public PolicyConstraintsExtension(Boolean critical, int require, int inhibit)
    throws IOException {
        this.require = require;
        this.inhibit = inhibit;
	this.extensionId = PKIXExtensions.PolicyConstraints_Id;
	this.critical = critical.booleanValue();
        encodeThis();
    }

    /**
     * Create the extension from its DER encoded value and criticality.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value an array of DER encoded bytes of the actual value.
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
    public PolicyConstraintsExtension(Boolean critical, Object value)
    throws IOException {
	this.extensionId = PKIXExtensions.PolicyConstraints_Id;
	this.critical = critical.booleanValue();

        this.extensionValue = (byte[]) value;
	DerValue val = new DerValue(this.extensionValue);
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Sequence tag missing for PolicyConstraint.");
        }
        DerInputStream in = val.data;
        while (in != null && in.available() != 0) {
            DerValue next = in.getDerValue();

            if (next.isContextSpecific(TAG_REQUIRE) && !next.isConstructed()) {
                if (this.require != -1)
                    throw new IOException("Duplicate requireExplicitPolicy" +
                          "found in the PolicyConstraintsExtension");
                next.resetTag(DerValue.tag_Integer);
                this.require = next.getInteger();

            } else if (next.isContextSpecific(TAG_INHIBIT) &&
                       !next.isConstructed()) {
                if (this.inhibit != -1)
                    throw new IOException("Duplicate inhibitPolicyMapping" +
                          "found in the PolicyConstraintsExtension");
                next.resetTag(DerValue.tag_Integer);
                this.inhibit = next.getInteger();
            } else
                throw new IOException("Invalid encoding of PolicyConstraint");
        }
    }

    /**
     * Return the extension as user readable string.
     */
    public String toString() {
        String s;
        s = super.toString() + "PolicyConstraints: [" + "  Require: ";
        if (require == -1)
            s += "unspecified;";
        else
            s += require + ";";
        s += "\tInhibit: ";
        if (inhibit == -1)
            s += "unspecified";
        else
            s += inhibit;
        s += " ]\n";
        return s;
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (extensionValue == null) {
	  extensionId = PKIXExtensions.PolicyConstraints_Id;
	  critical = false;
	  encodeThis();
	}
	super.encode(tmp);
	out.write(tmp.toByteArray());
    }

    /**
     * Set the attribute value.
     */
    public void set(String name, Object obj) throws IOException {
	if (!(obj instanceof Integer)) {
	    throw new IOException("Attribute value should be of type Integer.");
        }
	if (name.equalsIgnoreCase(REQUIRE)) {
	    require = ((Integer)obj).intValue();
	} else if (name.equalsIgnoreCase(INHIBIT)) {
	    inhibit = ((Integer)obj).intValue();
	} else {
	  throw new IOException("Attribute name " + "[" + name + "]" +
                                " not recognized by " +
				"CertAttrSet:PolicyConstraints.");
	}
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public Object get(String name) throws IOException {
	if (name.equalsIgnoreCase(REQUIRE)) {
	    return new Integer(require);
	} else if (name.equalsIgnoreCase(INHIBIT)) {
	    return new Integer(inhibit);
	} else {
	  throw new IOException("Attribute name not recognized by " +
				"CertAttrSet:PolicyConstraints.");
	}
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
	if (name.equalsIgnoreCase(REQUIRE)) {
	    require = -1;
	} else if (name.equalsIgnoreCase(INHIBIT)) {
	    inhibit = -1;
	} else {
	  throw new IOException("Attribute name not recognized by " +
				"CertAttrSet:PolicyConstraints.");
	}
        encodeThis();
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(REQUIRE);
        elements.addElement(INHIBIT);

	return (elements.elements());
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return (NAME);
    }
}