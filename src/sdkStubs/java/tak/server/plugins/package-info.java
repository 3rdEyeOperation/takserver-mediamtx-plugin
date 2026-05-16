/*
 * Compile-only stubs for the TAK Server plugin SDK.
 *
 * These declarations mirror just enough of the real
 * {@code gov.tak:takserver-plugins} artifact for this plugin to compile in
 * isolation. At runtime the TAK Server Plugin Manager supplies the real
 * implementations on the classloader, so these stubs are intentionally
 * excluded from the shaded plugin JAR (see build.gradle).
 *
 * Do NOT add behavior to these classes; they exist solely to satisfy javac.
 */
package tak.server.plugins;
