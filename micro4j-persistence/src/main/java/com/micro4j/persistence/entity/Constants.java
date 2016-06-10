package com.micro4j.persistence.entity;

import static java.util.Arrays.asList;

import java.util.List;

interface Constants {

    static final String ID = "ID";

    static final String CREATE_DATE = "CREATE_DATE";

    static final String UPDATE_DATE = "UPDATE_DATE";

    static final String UPDATED_BY = "UPDATED_BY";

    static final String CREATED_BY = "CREATED_BY";

    static final String ACTIVE = "ACTIVE";

    static final String ACTIVE_STATUS = "1";

    static final String PASSIVE_STATUS = "0";

    static final String DEFAULT_USER = "SYS";

    static final List<String> DEFAULT_COLUMNS = asList(
                                                    ID, CREATE_DATE,
                                                    UPDATE_DATE, CREATED_BY,
                                                    UPDATED_BY, ACTIVE
                                                );
}
