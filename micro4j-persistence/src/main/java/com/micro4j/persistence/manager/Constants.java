/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.micro4j.persistence.manager;

import static java.util.Arrays.asList;

import java.util.List;

interface Constants {

    static final String ID = "ID";

    static final String CREATE_DATE = "CREATE_DATE";

    static final String UPDATE_DATE = "UPDATE_DATE";

    static final String UPDATED_BY = "UPDATED_BY";

    static final String CREATED_BY = "CREATED_BY";

    static final String ACTIVE = "ACTIVE";

    static final String STATUS_ACTIVE = "1";

    static final String STATUS_PASSIVE = "0";

    static final String DEFAULT_USER = "SYS";

    static final List<String> DEFAULT_COLUMNS = asList(
                                                    ID, CREATE_DATE,
                                                    UPDATE_DATE, CREATED_BY,
                                                    UPDATED_BY, ACTIVE
                                                );
}
