package com.micro4j.persistence.alter;


public interface AlterListener {

    void onTableDrop(AlterEvent event);

    void onTableCreate(AlterEvent event);

    void onTableAlterColumn(AlterEvent event);
}
