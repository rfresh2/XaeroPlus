/**
 * Xaero's World Map — Cross-Version Data Normalizer &amp; Converter.
 *
 * <p>Reads region files from any Xaero format version (Minecraft 1.12.2 through 1.21.5+),
 * produces a canonical version-independent representation, and can re-serialize to any
 * target format version with full loss tracking.</p>
 *
 * <h2>Key components:</h2>
 * <ul>
 *   <li>{@link xaeroplus.util.normalizer.RegionNormalizer} — Main entry point (read/write/convert)</li>
 *   <li>{@link xaeroplus.util.normalizer.RegionBinaryReader} — Binary format parser (all versions)</li>
 *   <li>{@link xaeroplus.util.normalizer.RegionBinaryWriter} — Binary format serializer (any target version)</li>
 *   <li>{@link xaeroplus.util.normalizer.BlockStateFixers} — Forward + reverse block state fixer pipeline</li>
 *   <li>{@link xaeroplus.util.normalizer.BiomeTable} — Biome ID/rename resolution (forward + reverse)</li>
 *   <li>{@link xaeroplus.util.normalizer.VanillaStatesLoader} — Legacy numeric block state loader</li>
 *   <li>{@link xaeroplus.util.normalizer.DirectoryWalker} — Save directory enumeration</li>
 *   <li>{@link xaeroplus.util.normalizer.NormalizedRegion} — Output data model</li>
 *   <li>{@link xaeroplus.util.normalizer.LossReport} — Lossy operation tracking</li>
 *   <li>{@link xaeroplus.util.normalizer.CacheInvalidator} — Cache directory cleanup</li>
 * </ul>
 *
 * <h2>Conversion pipeline:</h2>
 * <pre>
 * Source (any version) → NormalizedRegion (canonical) → Target (chosen version)
 *                                ↓
 *                          JSON output
 * </pre>
 *
 * <h2>Lossy boundaries:</h2>
 * <ul>
 *   <li>NBT → numeric ID (major 1+ → 0): ~2000+ post-1.12.2 blocks lost</li>
 *   <li>12-bit → 9-bit height (minor 4+ → &lt;4): Y &lt; -256 and Y &gt; 255 lost</li>
 *   <li>topHeight discarded (minor 4+ → &lt;4)</li>
 *   <li>Wall tall/low → true (major 3+ → &lt;3)</li>
 *   <li>Biome many-to-one renames (major 6+ → &lt;6)</li>
 *   <li>Tile metadata discarded (minor 4+/6+/7+ → earlier)</li>
 * </ul>
 *
 * @see xaeroplus.util.normalizer.RegionNormalizer
 */
package xaeroplus.util.normalizer;
