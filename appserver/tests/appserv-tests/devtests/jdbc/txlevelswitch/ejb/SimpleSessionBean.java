/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.s1asdev.jdbc.txlevelswitch.ejb;

import jakarta.ejb.*;
import javax.naming.*;
import javax.sql.*;
import java.rmi.*;
import java.util.*;
import java.sql.*;

public class SimpleSessionBean implements SessionBean
{

    private SessionContext ctxt_;
    private InitialContext ic_; 
    public void setSessionContext(SessionContext context) {
        ctxt_ = context;
	try {
	    ic_ = new InitialContext();
	} catch( NamingException ne ) {
	    ne.printStackTrace();
	}
    }

    public void ejbCreate() throws CreateException {
    }

    /**
     * Get connection and do some database updates. Then throw
     * an exception before tx commits and ensure that the updates
     * did not happen
     */
    public boolean test1() throws Exception {
	DataSource ds = (DataSource)ic_.lookup("java:comp/env/DataSource");
	Connection conn1 = null;
	Statement stmt1 = null;
	ResultSet rs1 = null;
	int keyToCompare = 10;
	boolean passed = false;
	//clean the database
	try {
	    conn1 = ds.getConnection();
	    stmt1 = conn1.createStatement();
	    stmt1.executeUpdate( "DELETE FROM TXLEVELSWITCH");
	} catch( SQLException e) {
	    e.printStackTrace();
	    return false;
	} finally {
	    if (stmt1 != null) { 
	        try { stmt1.close(); } catch( Exception e1 ) {}
	    }
	    if (conn1 != null) { 
	        try { conn1.close(); } catch( Exception e1 ) {}
	    }
	}
	
	try {
	    //Call a method with tx=RequiresNew and have it throw
	    //an exception
	    updateDb( "TXLEVELSWITCH", keyToCompare );
	} catch( Exception e ) {
	    if (!( e instanceof EJBException )) {
	        e.printStackTrace();
		return false;
	    } else {
	        System.out.println("Caught expected EJBException");
	    }
	}
	try {
	    //since our updateDb threw an exception, we should not
	    //find the inserted value here
	    conn1 = ds.getConnection();
	    stmt1 = conn1.createStatement();
	    rs1 = stmt1.executeQuery("SELECT * FROM TXLEVELSWITCH"); 
	    while( rs1.next() ) {
	        int key = rs1.getInt("c_id");
		if ( key == 10 ) {
		    System.out.println("update happened successfully. It should have failed");
		    return false;
		}
	    }
	} catch (Exception e) {
	   e.printStackTrace(); 
	   return false;
	} finally {
	    if (rs1 != null ) {
	        try { rs1.close(); } catch( Exception e1 ) {}
	    }
	    if ( stmt1 != null ) {
	        try { stmt1.close(); } catch( Exception e1) {}    
	    }
	    if ( conn1 != null ) {
	        try { conn1.close(); } catch( Exception e1) {}    
	    }
	}

	return true;
    }
    
    private void updateDb( String table, int keyToInsert ) throws Exception {
        DataSource ds = (DataSource)ic_.lookup("java:comp/env/DataSource");
	Connection conn1 = null;
	Statement stmt1 = null;
	ResultSet rs1 = null;
	boolean passed = true;
	try {
	    conn1 = ds.getConnection();
	    stmt1 = conn1.createStatement();
	    stmt1.executeUpdate("INSERT INTO " +table+" VALUES ("
	        + keyToInsert+", 'ABCD')");
            throw new Exception("Rollback the tx");
	} catch( SQLException sqle ) {
	    throw sqle;
	} catch (Exception e) {
	   throw new EJBException( e ); 
	} finally {
	    if (rs1 != null ) {
	        try { rs1.close(); } catch( Exception e1 ) {}
	    }
	    if ( stmt1 != null ) {
	        try { stmt1.close(); } catch( Exception e1) {}    
	    }
	    if ( conn1 != null ) {
	        try { conn1.close(); } catch( Exception e1) {}    
	    }
	}


    }
    /**
     * Get connection with two non-xa datasources.
     * Do some work using both. Should throw an
     * exception (that we catch ) since 2 non-xa
     * resources cannot be mixed. This test is run
     * after converting the 2 connection-pools to LocaTransaction
     * so by catching the exception we are asserting taht this
     * changeover is indeed successful
     */
    public boolean test2() throws Exception {
        System.out.println("************IN TEST 2*************");    
	InitialContext ic = new InitialContext();
	DataSource ds1 = (DataSource)ic.lookup("java:comp/env/DataSource1");
	DataSource ds2 = (DataSource)ic.lookup("java:comp/env/DataSource2");
	Connection conn1 = null;
	Connection conn2 = null;
	Statement stmt1 = null;
	Statement stmt2 = null;
	ResultSet rs1 = null;
	ResultSet rs2 = null;
	boolean passed = true;
	try {
            System.out.println("Before getConnection 1");	
	    conn1 = ds1.getConnection();
            System.out.println("After getConnection 1");	
            System.out.println("Before getConnection 2");	
	    conn2 = ds2.getConnection();
            System.out.println("After getConnection 2");	
	    
            System.out.println("Before createStatement 1");	
	    stmt1 = conn1.createStatement();
            System.out.println("After createStatement 1");	
            System.out.println("Before createStatement 2");	
	    stmt2 = conn2.createStatement();
            System.out.println("After createStatement 2");	
            
            System.out.println("executing statement 1"); 

	    try {
	        rs1 = stmt1.executeQuery("SELECT * FROM TXLEVELSWITCH");
            } catch( Exception e2 ) {
	        System.out.println("Exception for first query :" + e2.getMessage() );
	    } finally {
	        passed = false;
            }
           
            System.out.println("executing statement 2"); 
	    try {
	        rs2 = stmt2.executeQuery("SELECT * FROM TXLEVELSWITCH2");
	    } catch( Exception e2) {
	        System.out.println("Exception for second query :" + e2.getMessage() );
	    } finally {
	        passed = false;
	    } 

            System.out.println("finished executing statements"); 
	    passed = !(rs1.next() & rs2.next());
	} catch (Exception e) {
	    passed = true;
	    System.out.println("final exception : " + e.getMessage() );
	    throw new EJBException(e);
	} finally {
	    if (rs1 != null ) {
	        try { rs1.close(); } catch( Exception e1 ) {}
	    }

	    if (rs2 != null ) {
	        try { rs2.close(); } catch( Exception e1 ) {}
	    }

	    if ( stmt1 != null ) {
	        try { stmt1.close(); } catch( Exception e1) {}    
	    }
	    if ( stmt2 != null ) {
	        try { stmt2.close(); } catch( Exception e1) {}    
	    }
	    if ( conn1 != null ) {
	        try { conn1.close(); } catch( Exception e1) {}    
	    }
	    if ( conn2 != null ) {
	        try { conn2.close(); } catch( Exception e1) {}    
	    }
	}
	return passed;
    }
    public void ejbLoad() {}
    public void ejbStore() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void unsetEntityContext() {}
    public void ejbPostCreate() {}
}
