package com.mycompany.bugtracker.repository;

import com.mycompany.bugtracker.domain.Ticket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Ticket entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TicketRepository extends R2dbcRepository<Ticket, Long>, TicketRepositoryInternal {
    Flux<Ticket> findAllBy(Pageable pageable);

    @Override
    Mono<Ticket> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Ticket> findAllWithEagerRelationships();

    @Override
    Flux<Ticket> findAllWithEagerRelationships(Pageable page);

    @Override
    Mono<Void> deleteById(Long id);

    @Query("SELECT * FROM ticket entity WHERE entity.project_id = :id")
    Flux<Ticket> findByProject(Long id);

    @Query("SELECT * FROM ticket entity WHERE entity.project_id IS NULL")
    Flux<Ticket> findAllWhereProjectIsNull();

    @Query("SELECT * FROM ticket entity WHERE entity.assigned_to_id = :id")
    Flux<Ticket> findByAssignedTo(Long id);

    @Query("SELECT * FROM ticket entity WHERE entity.assigned_to_id IS NULL")
    Flux<Ticket> findAllWhereAssignedToIsNull();

    @Query(
        "SELECT entity.* FROM ticket entity JOIN rel_ticket__label joinTable ON entity.id = joinTable.ticket_id WHERE joinTable.label_id = :id"
    )
    Flux<Ticket> findByLabel(Long id);

    // just to avoid having unambigous methods
    @Override
    Flux<Ticket> findAll();

    @Override
    Mono<Ticket> findById(Long id);

    @Override
    <S extends Ticket> Mono<S> save(S entity);
}

interface TicketRepositoryInternal {
    <S extends Ticket> Mono<S> insert(S entity);
    <S extends Ticket> Mono<S> save(S entity);
    Mono<Integer> update(Ticket entity);

    Flux<Ticket> findAll();
    Mono<Ticket> findById(Long id);
    Flux<Ticket> findAllBy(Pageable pageable);
    Flux<Ticket> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Ticket> findOneWithEagerRelationships(Long id);

    Flux<Ticket> findAllWithEagerRelationships();

    Flux<Ticket> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
