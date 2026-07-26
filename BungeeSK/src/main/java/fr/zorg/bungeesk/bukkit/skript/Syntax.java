package fr.zorg.bungeesk.bukkit.skript;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptEvent;
import fr.zorg.bungeesk.bukkit.BungeeSK;
import org.bukkit.event.Event;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

import java.util.function.Supplier;

/**
 * Thin wrapper around Skript's modern {@link SyntaxRegistry} so each syntax class can register itself
 * in a single line, the way {@code Skript.registerEffect(...)} / {@code registerExpression(...)} used
 * to read — those static methods are deprecated for removal. Keeping the registry calls in one place
 * means a future Skript API change is a one-file fix, not a sweep across every effect/expression.
 *
 * <p>Each syntax class still registers itself from its {@code static} initializer; the registry is
 * published by {@link BungeeSK} before it loads the {@code skript} package (which triggers those
 * initializers), so it is always available here.
 */
public final class Syntax {

    private Syntax() {
    }

    public static <E extends Effect> void effect(Class<E> type, Supplier<E> supplier, String... patterns) {
        registry().register(SyntaxRegistry.EFFECT, SyntaxInfo.simple(type, supplier, patterns));
    }

    public static <E extends Condition> void condition(Class<E> type, Supplier<E> supplier, String... patterns) {
        registry().register(SyntaxRegistry.CONDITION, SyntaxInfo.simple(type, supplier, patterns));
    }

    public static <E extends Section> void section(Class<E> type, Supplier<E> supplier, String... patterns) {
        registry().register(SyntaxRegistry.SECTION, SyntaxInfo.simple(type, supplier, patterns));
    }

    /**
     * Starts building an event registration. Chain {@code addDescription/addExamples/addSince} on the
     * returned builder, then pass it to {@link #registerEvent(BukkitSyntaxInfos.Event.Builder)}.
     */
    public static <E extends SkriptEvent> BukkitSyntaxInfos.Event.Builder<?, E> event(
            Class<E> type, Supplier<E> supplier, String name,
            Class<? extends Event> eventClass, String... patterns) {
        return BukkitSyntaxInfos.Event.builder(type, name)
                .supplier(supplier)
                .addEvent(eventClass)
                .addPatterns(patterns);
    }

    public static void registerEvent(BukkitSyntaxInfos.Event.Builder<?, ?> builder) {
        registry().register(BukkitSyntaxInfos.Event.KEY, builder.build());
    }

    /** Registers an expression with the default (simple) parse priority. */
    public static <E extends Expression<R>, R> void expression(Class<E> type, Supplier<E> supplier,
                                                               Class<R> returnType, String... patterns) {
        registry().register(SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.simple(type, supplier, returnType, patterns));
    }

    /**
     * Registers a property expression (e.g. {@code [the] address of %bungeeserver%}) via
     * {@link PropertyExpression}, which generates the two property patterns for you. Both of its static
     * {@code register} overloads are deprecated for removal in Skript 2.16, and the only non-deprecated
     * path is to hand-build a {@code DefaultSyntaxInfos.Expression} with the property patterns spelled
     * out — so the deprecation is suppressed here, in the single place every property expression funnels
     * through, rather than replicating Skript's pattern generation.
     */
    @SuppressWarnings("removal")
    public static <E extends Expression<R>, R> void property(Class<E> type, Class<R> returnType,
                                                             String property, String fromType) {
        PropertyExpression.register(registry(), type, returnType, property, fromType);
    }

    /** Registers an expression with an explicit parse priority (replaces the old {@code ExpressionType}). */
    public static <E extends Expression<R>, R> void expression(Class<E> type, Supplier<E> supplier,
                                                               Class<R> returnType, Priority priority,
                                                               String... patterns) {
        registry().register(SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(type, returnType)
                        .supplier(supplier)
                        .priority(priority)
                        .addPatterns(patterns)
                        .build());
    }

    private static SyntaxRegistry registry() {
        return BungeeSK.getSyntaxRegistry();
    }
}
