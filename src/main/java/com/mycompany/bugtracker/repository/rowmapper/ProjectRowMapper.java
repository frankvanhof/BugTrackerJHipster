package com.mycompany.bugtracker.repository.rowmapper;

import com.mycompany.bugtracker.domain.Project;
import com.mycompany.bugtracker.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Project}, with proper type conversions.
 */
@Service
public class ProjectRowMapper implements BiFunction<Row, String, Project> {

    private final ColumnConverter converter;

    public ProjectRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Project} stored in the database.
     */
    @Override
    public Project apply(Row row, String prefix) {
        Project entity = new Project();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setName(converter.fromRow(row, prefix + "_name", String.class));
        return entity;
    }
}
