package com.lenemon.muffin;

import com.cobblemon.mod.common.api.pokedex.Dexes;
import com.cobblemon.mod.common.api.pokedex.def.PokedexDef;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MuffinPoolCache {

    private static final Map<MagicMuffinType, CompiledPools> CACHE = new EnumMap<>(MagicMuffinType.class);

    private MuffinPoolCache() {}

    public static void rebuild(MinecraftServer server) {
        EnumMap<MagicMuffinType, CompiledPools> rebuilt = new EnumMap<>(MagicMuffinType.class);
        for (MagicMuffinType type : MagicMuffinType.values()) {
            MuffinConfig.MuffinTypeConfig cfg = MuffinConfig.get(type);
            rebuilt.put(type, new CompiledPools(
                    compilePool(cfg.pools.standard, false),
                    compilePool(cfg.pools.legendary, true)
            ));
        }
        CACHE.clear();
        CACHE.putAll(rebuilt);
        System.out.println("[Muffin] Cache reconstruit.");
    }

    public static List<Species> getCandidates(MagicMuffinType type, boolean legendaryPool) {
        CompiledPools pools = CACHE.get(type);
        if (pools == null) return List.of();
        return legendaryPool ? pools.legendary() : pools.standard();
    }

    private static List<Species> compilePool(MuffinConfig.PoolConfig pool, boolean legendaryPool) {
        Set<String> names = new LinkedHashSet<>();

        for (String regionId : pool.regions) {
            Identifier dexId = Identifier.of("cobblemon", regionId);
            PokedexDef def = Dexes.INSTANCE.getDexEntryMap().get(dexId);
            if (def == null) {
                System.err.println("[Muffin] Region inconnue ignoree : " + regionId);
                continue;
            }
            for (PokedexEntry entry : def.getEntries()) {
                Species species = PokemonSpecies.getByIdentifier(entry.getSpeciesId());
                if (species == null || !species.getImplemented()) continue;
                if (isLegendaryFamily(species) == legendaryPool) {
                    names.add(species.getResourceIdentifier().getPath().toLowerCase());
                }
            }
        }

        names.addAll(pool.species);
        names.removeAll(pool.excludeSpecies);

        List<Species> resolved = new ArrayList<>();
        for (String name : names) {
            Species species = resolveConfiguredSpecies(name);
            if (species == null) {
                System.err.println("[Muffin] Pokemon inconnu ignore : " + name);
                continue;
            }
            if (!species.getImplemented()) continue;
            if (isLegendaryFamily(species) != legendaryPool) continue;
            resolved.add(species);
        }
        return List.copyOf(resolved);
    }

    private static Species resolveConfiguredSpecies(String rawName) {
        if (rawName == null || rawName.isBlank()) return null;

        String normalized = canonicalizeName(rawName);
        for (Species species : PokemonSpecies.getSpecies()) {
            if (species == null) continue;
            String byDisplay = canonicalizeName(species.getName());
            String byId = species.getResourceIdentifier() != null
                    ? canonicalizeName(species.getResourceIdentifier().getPath())
                    : "";
            if (normalized.equals(byDisplay) || normalized.equals(byId)) {
                return species;
            }
        }

        try {
            return PokemonSpecies.getByName(sanitizeIdentifierPath(rawName));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String canonicalizeName(String raw) {
        return sanitizeIdentifierPath(raw)
                .replace("-", "")
                .replace("_", "");
    }

    private static String sanitizeIdentifierPath(String raw) {
        String value = raw.trim().toLowerCase();
        int namespaceSep = value.indexOf(':');
        if (namespaceSep >= 0 && namespaceSep + 1 < value.length()) {
            value = value.substring(namespaceSep + 1);
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\'':
                case '’':
                case '.':
                case ' ':
                    break;
                case '♀':
                    builder.append('f');
                    break;
                case '♂':
                    builder.append('m');
                    break;
                default:
                    if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '/' || c == '.') {
                        builder.append(c);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    private static boolean isLegendaryFamily(Species species) {
        Pokemon preview = species.create(1);
        return preview.isLegendary() || preview.isMythical() || preview.isUltraBeast() || species.getLabels().contains("legendary");
    }

    private record CompiledPools(List<Species> standard, List<Species> legendary) {}
}
