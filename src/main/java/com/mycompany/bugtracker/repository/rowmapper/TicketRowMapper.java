package com.mycompany.bugtracker.repository.rowmapper;

import com.mycompany.bugtracker.domain.Ticket;
import com.mycompany.bugtracker.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.time.LocalDate;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Ticket}, with proper type conversions.
 */
@Service
public class TicketRowMapper implements BiFunction<Row, String, Ticket> {

    private final ColumnConverter converter;

    public TicketRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Ticket} stored in the database.
     */
    @Override
    public Ticket apply(Row row, String prefix) {
        Ticket entity = new Ticket();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setDueDate(converter.fromRow(row, prefix + "_due_date", LocalDate.class));
        entity.setDone(converter.fromRow(row, prefix + "_done", Boolean.class));
        entity.setProjectId(converter.fromRow(row, prefix + "_project_id", Long.class));
        entity.setAssignedToId(converter.fromRow(row, prefix + "_assigned_to_id", Long.class));
        return entity;
    }
}
