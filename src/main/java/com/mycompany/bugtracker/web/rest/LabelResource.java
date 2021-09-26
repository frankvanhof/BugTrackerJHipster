package com.mycompany.bugtracker.web.rest;

import com.mycompany.bugtracker.domain.Label;
import com.mycompany.bugtracker.repository.LabelRepository;
import com.mycompany.bugtracker.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.bugtracker.domain.Label}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class LabelResource {

    private final Logger log = LoggerFactory.getLogger(LabelResource.class);

    private static final String ENTITY_NAME = "label";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LabelRepository labelRepository;

    public LabelResource(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    /**
     * {@code POST  /labels} : Create a new label.
     *
     * @param label the label to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new label, or with status {@code 400 (Bad Request)} if the label has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/labels")
    public Mono<ResponseEntity<Label>> createLabel(@Valid @RequestBody Label label) throws URISyntaxException {
        log.debug("REST request to save Label : {}", label);
        if (label.getId() != null) {
            throw new BadRequestAlertException("A new label cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return labelRepository
            .save(label)
            .map(result -> {
                try {
                    return ResponseEntity
                        .created(new URI("/api/labels/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /labels/:id} : Updates an existing label.
     *
     * @param id the id of the label to save.
     * @param label the label to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated label,
     * or with status {@code 400 (Bad Request)} if the label is not valid,
     * or with status {@code 500 (Internal Server Error)} if the label couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/labels/{id}")
    public Mono<ResponseEntity<Label>> updateLabel(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Label label
    ) throws URISyntaxException {
        log.debug("REST request to update Label : {}, {}", id, label);
        if (label.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, label.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return labelRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return labelRepository
                    .save(label)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity
                            .ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /labels/:id} : Partial updates given fields of an existing label, field will ignore if it is null
     *
     * @param id the id of the label to save.
     * @param label the label to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated label,
     * or with status {@code 400 (Bad Request)} if the label is not valid,
     * or with status {@code 404 (Not Found)} if the label is not found,
     * or with status {@code 500 (Internal Server Error)} if the label couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/labels/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<Label>> partialUpdateLabel(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Label label
    ) throws URISyntaxException {
        log.debug("REST request to partial update Label partially : {}, {}", id, label);
        if (label.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, label.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return labelRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<Label> result = labelRepository
                    .findById(label.getId())
                    .map(existingLabel -> {
                        if (label.getLabel() != null) {
                            existingLabel.setLabel(label.getLabel());
                        }

                        return existingLabel;
                    })
                    .flatMap(labelRepository::save);

                return result
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(res ->
                        ResponseEntity
                            .ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, res.getId().toString()))
                            .body(res)
                    );
            });
    }

    /**
     * {@code GET  /labels} : get all the labels.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of labels in body.
     */
    @GetMapping("/labels")
    public Mono<List<Label>> getAllLabels() {
        log.debug("REST request to get all Labels");
        return labelRepository.findAll().collectList();
    }

    /**
     * {@code GET  /labels} : get all the labels as a stream.
     * @return the {@link Flux} of labels.
     */
    @GetMapping(value = "/labels", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Label> getAllLabelsAsStream() {
        log.debug("REST request to get all Labels as a stream");
        return labelRepository.findAll();
    }

    /**
     * {@code GET  /labels/:id} : get the "id" label.
     *
     * @param id the id of the label to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the label, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/labels/{id}")
    public Mono<ResponseEntity<Label>> getLabel(@PathVariable Long id) {
        log.debug("REST request to get Label : {}", id);
        Mono<Label> label = labelRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(label);
    }

    /**
     * {@code DELETE  /labels/:id} : delete the "id" label.
     *
     * @param id the id of the label to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/labels/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteLabel(@PathVariable Long id) {
        log.debug("REST request to delete Label : {}", id);
        return labelRepository
            .deleteById(id)
            .map(result ->
                ResponseEntity
                    .noContent()
                    .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                    .build()
            );
    }
}
