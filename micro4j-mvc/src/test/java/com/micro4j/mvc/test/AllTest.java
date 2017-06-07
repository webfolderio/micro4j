package com.micro4j.mvc.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TemplateEngineTest.class,
    ResteasyTest.class,
    HtmlEscapeTest.class
})
public class AllTest {

}
