@file:OptIn(ExperimentalMultiplatform::class)

package moe.rukamori.archivetune.compat

/**
 * @Synchronized di Java solo dove esiste (Android). Su iPhone l'annotazione non fa nulla:
 * le classi che la usano proteggono comunque le proprie mappe con [synchronized].
 */
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
expect annotation class Synchronized()
