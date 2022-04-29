(ns polylith.clj.core.help.check
     (:require [polylith.clj.core.help.shared :as s]
               [polylith.clj.core.util.interface.color :as color]))

(defn help-text [cm]
  (str "  Validates the workspace.\n"
       "\n"
       "  poly check\n"
       "\n"
       "  Prints 'OK' and returns 0 if no errors were found.\n"
       "  If errors or warnings were found, show messages and return the error code,\n"
       "  or 0 if only warnings. If internal errors, 1 is returned.\n"
       "\n"
       "  " (color/error cm "Error 101") " - Illegal dependency on namespace.\n"
       "    Triggered if a " (s/key ":require" cm) " statement refers to a component namespace\n"
       "    other than " (color/interface "interface" cm) ". Examples of valid namespaces:\n"
       "     - " (s/component-ns "interface" cm) "\n"
       "     - " (s/component-ns "interface.subns" cm) "\n"
       "     - " (s/component-ns "interface.my.subns" cm) "\n"
       "\n"
       "  " (color/error cm "Error 102") " - Function or macro is defined twice.\n"
       "    Triggered if a function or macro is defined twice in the same namespace.\n"
       "\n"
       "  " (color/error cm "Error 103") " - Missing definitions.\n"
       "    Triggered if a " (color/green cm "def") ", " (color/green cm "defn") " or "
       (color/green cm "defmacro") " definition exists in one component's\n"
       "    interface but is missing in another component that uses the same interface.\n"
       "\n"
       "  " (color/error cm "Error 104") " - Circular dependencies.\n"
       "    Triggered if circular dependencies were detected, e.g.:\n"
       "    Component A depends on B that depends on A (A > B > A), or A > B > C > A.\n"
       "\n"
       "  " (color/error cm "Error 105") " - Illegal name sharing.\n"
       "    Triggered if a base has the same name as a component or interface.\n"
       "    Projects and profiles can be given any name.\n"
       "\n"
       "  " (color/error cm "Error 106") " - Multiple components that share the same interfaces in a project.\n"
       "    Triggered if a project contains more than one component that shares the\n"
       "    same interface.\n"
       "\n"
       "  " (color/error cm "Error 107") " - Missing components in project.\n"
       "    Triggered if a component depends on an interface that is not included in the\n"
       "    project. The solution is to add a component to the project that\n"
       "    implements the interface.\n"
       "\n"
       "  " (color/error cm "Error 108") " - Components with an interface that is implemented by more than one\n"
       "              component are not allowed for the development project.\n"
       "    The solution is to remove the component from the development project\n"
       "    and define the paths for each component in separate profiles\n"
       "    (including test paths).\n"
       "\n"
       "  " (color/error cm "Error 109") " - Invalid test runner configuration for some projects.\n"
       "    The value of the optional :create-test-runner key under [:test] or\n"
       "    [:projects \"some-project-name\" :test] in workspace.edn must be either\n"
       "    nil, :default, or a fully qualified symbol referring to a function on\n"
       "    the poly tool's classpath, which can take a single argument and must return\n"
       "    an instance of polylith.clj.core.test-runner-contract.interface/TestRunner.\n"
       "\n"
       "  " (color/warning cm "Warning 201") " - Mismatching parameter lists in function or macro.\n"
       "    Triggered if a function or macro is defined in the interface for a component\n"
       "    but also defined in the same interface for another component but with a\n"
       "    different parameter list.\n"
       "\n"
       "  " (color/warning cm "Warning 202") " - Missing paths in project.\n"
       "    Triggered if a path in a project doesn't exist on disk.\n"
       "    The solution is to add the file or directory, or to remove the path.\n"
       "\n"
       "  " (color/warning cm "Warning 203") " - Path exists in both dev and profile.\n"
       "    It's discouraged to have the same path in both the development project\n"
       "    and a profile. The solution is to remove the path from dev or the profile.\n"
       "\n"
       "  " (color/warning cm "Warning 205") " - Non top namespace was found in brick.\n"
       "    Triggered if a namespace in a brick doesn't start with the top namespaces\n"
       "    defined in " (s/key ":top-namespace" cm) " in ./workspace.edn."))

(defn print-help [cm]
  (-> cm help-text println))
