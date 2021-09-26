package com.mycompany.bugtracker.repository;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import com.mycompany.bugtracker.domain.Label;
import com.mycompany.bugtracker.domain.Ticket;
import com.mycompany.bugtracker.repository.rowmapper.ProjectRowMapper;
import com.mycompany.bugtracker.repository.rowmapper.TicketRowMapper;
import com.mycompany.bugtracker.repository.rowmapper.UserRowMapper;
import com.mycompany.bugtracker.service.EntityManager;
import com.mycompany.bugtracker.service.EntityManager.LinkTable;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoinCondition;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive custom repository implementation for the Ticket entity.
 */
@SuppressWarnings("unused")
class TicketRepositoryInternalImpl implements TicketRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final ProjectRowMapper projectMapper;
    private final UserRowMapper userMapper;
    private final TicketRowMapper ticketMapper;

    private static final Table entityTable = Table.aliased("ticket", EntityManager.ENTITY_ALIAS);
    private static final Table projectTable = Table.aliased("project", "project");
    private static final Table assignedToTable = Table.aliased("jhi_user", "assignedTo");

    private static final EntityManager.LinkTable labelLink = new LinkTable("rel_ticket__label", "ticket_id", "label_id");

    public TicketRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        ProjectRowMapper projectMapper,
        UserRowMapper userMapper,
        TicketRowMapper ticketMapper
    ) {
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public Flux<Ticket> findAllBy(Pageable pageable) {
        return findAllBy(pageable, null);
    }

    @Override
    public Flux<Ticket> findAllBy(Pageable pageable, Criteria criteria) {
        return createQuery(pageable, criteria).all();
    }

    RowsFetchSpec<Ticket> createQuery(Pageable pageable, Criteria criteria) {
        List<Expression> columns = TicketSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(ProjectSqlHelper.getColumns(projectTable, "project"));
        columns.addAll(UserSqlHelper.getColumns(assignedToTable, "assignedTo"));
        SelectFromAndJoinCondition selectFrom = Select
            .builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(projectTable)
            .on(Column.create("project_id", entityTable))
            .equals(Column.create("id", projectTable))
            .leftOuterJoin(assignedToTable)
            .on(Column.create("assigned_to_id", entityTable))
            .equals(Column.create("id", assignedToTable));

        String select = entityManager.createSelect(selectFrom, Ticket.class, pageable, criteria);
        String alias = entityTable.getReferenceName().getReference();
        String selectWhere = Optional
            .ofNullable(criteria)
            .map(crit ->
                new StringBuilder(select)
                    .append(" ")
                    .append("WHERE")
                    .append(" ")
                    .append(alias)
                    .append(".")
                    .append(crit.toString())
                    .toString()
            )
            .orElse(select); // TODO remove once https://github.com/spring-projects/spring-data-jdbc/issues/907 will be fixed
        return db.sql(selectWhere).map(this::process);
    }

    @Override
    public Flux<Ticket> findAll() {
        return findAllBy(null, null);
    }

    @Override
    public Mono<Ticket> findById(Long id) {
        return createQuery(null, where("id").is(id)).one();
    }

    @Override
    public Mono<Ticket> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Ticket> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Ticket> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Ticket process(Row row, RowMetadata metadata) {
        Ticket entity = ticketMapper.apply(row, "e");
        entity.setProject(projectMapper.apply(row, "project"));
        entity.setAssignedTo(userMapper.apply(row, "assignedTo"));
        return entity;
    }

    @Override
    public <S extends Ticket> Mono<S> insert(S entity) {
        return entityManager.insert(entity);
    }

    @Override
    public <S extends Ticket> Mono<S> save(S entity) {
        if (entity.getId() == null) {
            return insert(entity).flatMap(savedEntity -> updateRelations(savedEntity));
        } else {
            return update(entity)
                .map(numberOfUpdates -> {
                    if (numberOfUpdates.intValue() <= 0) {
                        throw new IllegalStateException("Unable to update Ticket with id = " + entity.getId());
                    }
                    return entity;
                })
                .then(updateRelations(entity));
        }
    }

    @Override
    public Mono<Integer> update(Ticket entity) {
        //fixme is this the proper way?
        return r2dbcEntityTemplate.update(entity).thenReturn(1);
    }

    @Override
    public Mono<Void> deleteById(Long entityId) {
        return deleteRelations(entityId)
            .then(r2dbcEntityTemplate.delete(Ticket.class).matching(query(where("id").is(entityId))).all().then());
    }

    protected <S extends Ticket> Mono<S> updateRelations(S entity) {
        Mono<Void> result = entityManager.updateLinkTable(labelLink, entity.getId(), entity.getLabels().stream().map(Label::getId)).then();
        return result.thenReturn(entity);
    }

    protected Mono<Void> deleteRelations(Long entityId) {
        return entityManager.deleteFromLinkTable(labelLink, entityId);
    }
}

class TicketSqlHelper {

    static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("title", table, columnPrefix + "_title"));
        columns.add(Column.aliased("description", table, columnPrefix + "_description"));
        columns.add(Column.aliased("due_date", table, columnPrefix + "_due_date"));
        columns.add(Column.aliased("done", table, columnPrefix + "_done"));

        columns.add(Column.aliased("project_id", table, columnPrefix + "_project_id"));
        columns.add(Column.aliased("assigned_to_id", table, columnPrefix + "_assigned_to_id"));
        return columns;
    }
}
