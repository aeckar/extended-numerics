module aeckar.numerics.core {
    requires transitive kotlin.stdlib;
    requires transitive static kotlinx.serialization.core;

    exports io.github.aeckar;
    exports io.github.aeckar.numerics;
    exports io.github.aeckar.numerics.functions;
}