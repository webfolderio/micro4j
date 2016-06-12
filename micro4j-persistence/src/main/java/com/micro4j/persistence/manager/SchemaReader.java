package com.micro4j.persistence.manager;

import java.io.InputStream;

import com.micro4j.persistence.core.SchemaDefinition;

public interface SchemaReader {

    SchemaDefinition read(InputStream is);
}
