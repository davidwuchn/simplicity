Theme/Workspace/Configuration: A workspace is a single place for all of your organizations code and projects

Task/Component/Blocks: A component is an encapsulated block of code that can be assembled together with a base and a set of other components into services, libraries or tools. Components achieve encapsulation and composability be separating their private implementation from their public interface:

Story/Base/API: Bases are the building blocks that exposes a public API to outside world. A base is an encapsulated block of code that can be assembled together with a set of components into services, libraries or tools. Bases achieve encapsulation and composability by separating their private implementation from their public API. 
A base has one role and that is to be a bridge between the outside world and the logic that performs the "real work", our components, Bases don't perform any business logic themselves, they only delegate to components.

Feature/Project/Artifacts: Projects configure Polylith's deployable artifacts. A project is the result of combining one base with multiple components and libraries.

Initiative/Development project/Environment to REPL: The development project is what we open in our editor/IDE and where we work with our entire codebase. The development project is where we specify all the components, bases and libraries that want to work with:
The development project gives us a delightful development experience that allows us to work with all our code from one place in a single REPL
