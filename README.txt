genome-observatory is a desktop application that runs and visualizes
neuroevolution: a population of small creatures, each driven by an evolving
neural-network brain, lives in a 2D survival world and is bred across
generations by a genetic algorithm. It is written in Java as a Gradle
multi-module project with a libGDX desktop client. It is a learning project,
not a product.

The simulation core is a pure, deterministic engine: a NEAT-capable graph
genome, a genetic algorithm (tournament selection, elitism, Gaussian weight
mutation, with an optional NEAT mode that evolves network topology through
speciation), and a tick-based world covering movement, two-eye vision sensing,
eating, spike combat, aging, death, and fitness. Two species co-evolve --
herbivores gain energy from food, carnivores hunt them with a spike -- and every
run is reproducible from its seed. A headless runner plays N generations and
prints per-generation stats; a Javalin server ticks the engine, streams full
world snapshots over WebSocket, persists each run to SQLite, and serves a REST
history API; the libGDX client renders the live world and an in-app analytics
view with fitness graphs, run comparison, and the champion's network topology.

Running it needs JDK 25 (pinned via mise and the Gradle toolchain) and the
bundled Gradle wrapper. Start the server and then the client in two terminals:

    ./gradlew :server:run
    ./gradlew :client:run

Or run it headless from a seed, with optional carnivores:

    ./gradlew :simulation-runner:run --args="--seed 42 --generations 100 --carnivores 20"

Licensed under the MIT License; see LICENSE.
