package wraith.alloyforgery.data;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.DependencySorter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Version of {@link TagLoader} but with tweaks for delayed use and without entire tags being thrown out
 *
 * @param <T>
 */
public class DelayedTagGroupLoader<T> extends TagLoader<T> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private Function<ResourceLocation, Optional<? extends T>> registryGetter = null;
    private final String dataType;

    public DelayedTagGroupLoader(String dataType) {
        super(identifier -> Optional.empty(), dataType);

        this.dataType = dataType;
    }

    public DelayedTagGroupLoader<T> setGetter(Function<ResourceLocation, Optional<? extends T>> registryGetter) {
        this.registryGetter = registryGetter;

        return this;
    }

    // Copy of vanilla but returns both the error list and the tag resolved to its best effort
    private Tuple<Collection<EntryWithSource>, Collection<T>> resolveAll(TagEntry.Lookup<T> valueGetter, List<EntryWithSource> entries) {
        ImmutableSet.Builder<T> builder = ImmutableSet.builder();
        List<EntryWithSource> list = new ArrayList();

        for (EntryWithSource trackedEntry : entries) {
            if (!trackedEntry.entry().build(valueGetter, builder::add)) {
                list.add(trackedEntry);
            }
        }

        return new Tuple<>(list, builder.build());
    }

    // Copy to vanilla but checks if this versions registeryGetter is set and adjusts
    // error handling to log the error without throwing the entire tag out
    @Override
    public Map<ResourceLocation, Collection<T>> build(Map<ResourceLocation, List<EntryWithSource>> tags) {
        if (registryGetter == null)
            throw new RuntimeException("DelayedTagGroupLoader did not have the required registeryGetter set to resolve! [Type: " + this.dataType + "]");

        final Map<ResourceLocation, Collection<T>> map = Maps.newHashMap();

        TagEntry.Lookup<T> valueGetter = new TagEntry.Lookup<>() {
            @Nullable
            @Override
            public T element(ResourceLocation id) {
                return registryGetter.apply(id).orElse(null);
            }

            @Nullable
            @Override
            public Collection<T> tag(ResourceLocation id) {
                return map.get(id);
            }
        };

        DependencySorter<ResourceLocation, TagLoader.SortingEntry> dependencyTracker = new DependencySorter<>();

        tags.forEach((id, entries) -> dependencyTracker.addEntry(id, new TagLoader.SortingEntry(entries)));

        dependencyTracker.orderByDependencies((id, dependencies) -> {
            var pair = this.resolveAll(valueGetter, dependencies.entries());

            var missingReferences = pair.getA();

            if (!missingReferences.isEmpty()) {
                LOGGER.error(
                        "Couldn't load the given entries within tag {}: {}",
                        id,
                        missingReferences.stream().map(Objects::toString).collect(Collectors.joining(", "))
                );
            }

            map.put(id, pair.getB());
        });

        return map;
    }
}
