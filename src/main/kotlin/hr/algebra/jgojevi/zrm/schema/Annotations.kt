package hr.algebra.jgojevi.zrm.schema

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
annotation class ForeignKey(val property: String, val action: Action = Action.CASCADE) {
    // TODO: ForeignKey actions are ignored
    enum class Action {
        NO_ACTION, RESTRICT, CASCADE, SET_NULL, SET_DEFAULT
    }
}