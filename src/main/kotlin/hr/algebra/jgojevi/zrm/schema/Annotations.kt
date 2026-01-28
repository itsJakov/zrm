package hr.algebra.jgojevi.zrm.schema

import kotlinx.serialization.Serializable

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(val name: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val name: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Key

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForeignKey(val property: String, val onUpdate: Action = Action.CASCADE, val onDelete: Action = Action.CASCADE) {
    @Serializable
    enum class Action(val sql: String) {
        NO_ACTION("no action"),
        RESTRICT("restrict"),
        CASCADE("cascade"),
        SET_NULL("set null"),
        SET_DEFAULT("set default");
    }
}