(ns polylith.clj.core.workspace-clj.core
  (:require [clojure.string :as str]
            [polylith.clj.core.common.interface :as common]
            [polylith.clj.core.file.interface :as file]
            [polylith.clj.core.git.interface :as git]
            [polylith.clj.core.util.interface :as util]
            [polylith.clj.core.util.interface.color :as color]
            [polylith.clj.core.user-config.interface :as user-config]
            [polylith.clj.core.version.interface :as version]
            [polylith.clj.core.path-finder.interface :as path-finder]
            [polylith.clj.core.workspace-clj.config :as config]
            [polylith.clj.core.workspace-clj.profile :as profile]
            [polylith.clj.core.workspace-clj.ws-reader :as ws-reader]
            [polylith.clj.core.workspace-clj.leiningen.core :as leiningen]
            [polylith.clj.core.workspace-clj.non-top-namespace :as non-top-ns]
            [polylith.clj.core.workspace-clj.bases-from-disk :as bases-from-disk]
            [polylith.clj.core.workspace-clj.projects-from-disk :as projects-from-disk]
            [polylith.clj.core.workspace-clj.components-from-disk :as components-from-disk]))

(defn stringify-key-value [[k v]]
  [(str k) (str v)])

(defn stringify [ws-type ns-to-lib]
  (when (not= ws-type :toolsdeps2)
    (into {} (mapv stringify-key-value ns-to-lib))))

(defn git-root []
  (let [[ok? root-path] (git/root-dir)]
    (if ok?
      root-path
      :no-git-root)))

(defn git-info [ws-dir vcs stable-tag-pattern branch]
  (let [current-branch (or branch (git/current-branch))]
    {:name                vcs
     :polylith-repo       git/repo
     :branch              current-branch
     :git-root            (git-root)
     :latest-polylith-sha (git/latest-polylith-sha current-branch)
     :stable-since        (git/latest-stable ws-dir stable-tag-pattern)}))

(defn ws-local-dir
  "Returns the directory/path to the workspace if it lives
   inside a git repository, or nil if the workspace and the
   git repository lives in the same directory."
  [ws-dir]
  (let [root-dir (git-root)]
    (when (and (not= ws-dir root-dir)
               (str/starts-with? ws-dir root-dir))
      (subs ws-dir (-> root-dir count inc)))))

(defn toolsdeps-ws-from-disk [ws-dir
                              ws-type
                              user-input
                              color-mode]
  (let [{:keys [aliases polylith]} (config/dev-config-from-disk ws-dir ws-type color-mode)
        ws-config (if (= :toolsdeps2 ws-type)
                    (config/ws-config-from-disk ws-dir color-mode)
                    (config/ws-config-from-dev polylith))
        {:keys [vcs top-namespace ws-type interface-ns default-profile-name release-tag-pattern stable-tag-pattern ns-to-lib compact-views]
         :or {vcs "git"
              release-tag-pattern "v[0-9]*"
              stable-tag-pattern "stable-*"
              compact-views {}}} ws-config
        interface-namespace (or interface-ns "interface")
        top-src-dir (-> top-namespace common/suffix-ns-with-dot common/ns-to-path)
        empty-character (user-config/empty-character)
        m2-dir (user-config/m2-dir)
        user-home (user-config/home-dir)
        thousand-separator (user-config/thousand-separator)
        user-config-filename (str (user-config/home-dir) "/.polylith/config.edn")
        brick->non-top-namespaces (non-top-ns/brick->non-top-namespaces ws-dir top-namespace)
        project->settings (:projects ws-config {})
        projects (projects-from-disk/read-projects ws-dir ws-type project->settings user-home color-mode)
        ns-to-lib-str (stringify ws-type (or ns-to-lib {}))
        components (components-from-disk/read-components ws-dir ws-type user-home top-namespace ns-to-lib-str top-src-dir interface-namespace brick->non-top-namespaces)
        bases (bases-from-disk/read-bases ws-dir ws-type user-home top-namespace ns-to-lib-str top-src-dir brick->non-top-namespaces)
        profile-to-settings (profile/profile-to-settings aliases user-home)
        paths (path-finder/paths ws-dir projects profile-to-settings)
        default-profile (or default-profile-name "default")
        active-profiles (profile/active-profiles user-input default-profile profile-to-settings)

        settings (util/ordered-map :version version/version
                                   :ws-type ws-type
                                   :ws-schema-version version/ws-schema-version
                                   :vcs (git-info ws-dir vcs stable-tag-pattern (:branch user-input))
                                   :top-namespace top-namespace
                                   :interface-ns interface-namespace
                                   :default-profile-name default-profile
                                   :active-profiles active-profiles
                                   :release-tag-pattern release-tag-pattern
                                   :stable-tag-pattern stable-tag-pattern
                                   :color-mode color-mode
                                   :compact-views compact-views
                                   :user-config-filename user-config-filename
                                   :empty-character empty-character
                                   :thousand-separator thousand-separator
                                   :profile-to-settings profile-to-settings
                                   :projects project->settings
                                   :ns-to-lib ns-to-lib-str
                                   :user-home user-home
                                   :m2-dir m2-dir)]

    (util/ordered-map :ws-dir ws-dir
                      :ws-local-dir (ws-local-dir ws-dir)
                      :ws-reader ws-reader/reader
                      :user-input user-input
                      :settings settings
                      :components components
                      :bases bases
                      :projects projects
                      :paths paths)))

(defn workspace-from-disk [user-input]
  (let [color-mode (or (:color-mode user-input) (user-config/color-mode) color/none)
        ws-dir (common/workspace-dir user-input color-mode)
        lein-config (str ws-dir "/project.clj")
        ws-type (cond
                  (file/exists (str ws-dir "/workspace.edn")) :toolsdeps2
                  (file/exists (str ws-dir "/deps.edn")) :toolsdeps1
                  (file/exists lein-config) :leiningen1)]
    (case ws-type
      nil nil
      :leiningen1 (leiningen/workspace-from-disk ws-dir user-input)
      (toolsdeps-ws-from-disk ws-dir ws-type user-input color-mode))))
