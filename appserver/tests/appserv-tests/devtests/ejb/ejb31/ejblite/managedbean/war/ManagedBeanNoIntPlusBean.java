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

package com.acme;

import jakarta.annotation.*;
import javax.interceptor.Interceptors;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

@ManagedBean("ManagedBeanNoIntPlusBean")
public class ManagedBeanNoIntPlusBean extends ManagedBeanSuper {

    static private int numInstances = 0;
    static private int numInterceptorInstances = 0;

    private String aroundInvoke = "";

    public void newInterceptorInstance() {
	numInterceptorInstances++;
    }

    public int getNumInstances() {
	return numInstances;
    }

    public int getNumInterceptorInstances() {
	return numInterceptorInstances;
    }

     @AroundInvoke
    private Object roundInvoke(InvocationContext c) throws Exception {
	System.out.println("In ManagedBeanNoIntPlusBean::roundInvoke() ");
	if( c.getMethod().getName().equals("getAroundInvokeSequence") ) {
	    String result = (String) c.proceed();
	    return "M" + result;
	} else {
	    return c.proceed();
	}
    }

    @PostConstruct
    private void init() {
	numInstances++;
    }

    @PreDestroy
    private void destroy() {
    }

}