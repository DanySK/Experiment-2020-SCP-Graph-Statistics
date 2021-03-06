/*
 * Copyright (C) 2010-2019, Danilo Pianini and contributors listed in the main project's alchemist/build.gradle file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.loader.export;

import com.google.common.collect.Lists;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Incarnation;
import it.unibo.alchemist.model.interfaces.Molecule;
import it.unibo.alchemist.model.interfaces.Reaction;
import it.unibo.alchemist.model.interfaces.Time;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Reads the value of a molecule and logs it.
 *
 */
public final class MoleculeReader implements Extractor {

    private final List<UnivariateStatistic> aggregators;
    private final List<String> columns;
    private final String property;
    private final Molecule mol;
    private final FilteringPolicy filter;

    /**
     * @param molecule
     *            the target molecule
     * @param property
     *            the target property
     * @param incarnation
     *            the target incarnation
     * @param filter
     *            the {@link FilteringPolicy} to use
     * @param aggregators
     *            the names of the {@link UnivariateStatistic} to use for
     *            aggregating data. If an empty list is passed, then the values
     *            will be logged indipendently for each node.
     */
    public MoleculeReader(final String molecule,
                          final String property,
                          final Incarnation<?, ?> incarnation,
                          final FilteringPolicy filter,
                          final List<String> aggregators) {
        this.property = property;
        this.mol = incarnation.createMolecule(molecule);
        this.filter = Objects.requireNonNull(filter);
        this.aggregators = Objects.requireNonNull(aggregators).parallelStream()
                .map(StatUtil::makeUnivariateStatistic)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        final var propertyText = property == null || property.isEmpty()
                ? ""
                : property.replaceAll("[^\\d\\w]*", "");
        final var shortProp = propertyText.isEmpty() ? "" : propertyText.substring(0, Math.min(5, propertyText.length())) + "@";
        this.columns = Collections.unmodifiableList(
                this.aggregators.isEmpty()
                        ? Lists.newArrayList(shortProp + molecule + "@every_node")
                        : this.aggregators.stream()
                        .map(a -> shortProp + molecule + '[' + a.getClass().getSimpleName() + ']')
                        .collect(Collectors.toList())
        );
    }

    @Override
    public <T> double[] extractData(
        final Environment<T, ?> environment,
        final Reaction<T> r,
        final Time time,
        final long step
    ) {
        @SuppressWarnings("unchecked")
        final DoubleStream values = environment.getNodes().stream()
                .mapToDouble(node ->
                    environment.getIncarnation()
                        .map(incarnation -> incarnation.getProperty(node, mol, property))
                        .orElseThrow(() -> new IllegalStateException("No incarnation available in the environment"))
                );
        if (aggregators.isEmpty()) {
            return values.toArray();
        } else {
            final double[] input = values.flatMap(filter::apply).toArray();
            if (input.length == 0) {
                final double[] result = new double[aggregators.size()];
                Arrays.fill(result, Double.NaN);
                return result;
            }
            return aggregators.stream()
                    .mapToDouble(a -> a.evaluate(input))
                    .toArray();
        }
    }

    @Override
    public List<String> getNames() {
        return columns;
    }

}
