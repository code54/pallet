(ns pallet.actions.direct.directory-test
  (:require
   [clojure.test :refer :all]
   [pallet.action :refer [action-fn]]
   [pallet.actions :refer [directories directory]]
   [pallet.build-actions :refer [build-actions]]
   [pallet.common.logging.logutils :refer [logging-threshold-fixture]]
   [pallet.stevedore :as stevedore]
   [pallet.test-utils
    :refer [with-bash-script-language with-ubuntu-script-template
            with-no-source-line-comments]]))

(use-fixtures
 :once
 with-ubuntu-script-template
 with-bash-script-language
 with-no-source-line-comments
 (logging-threshold-fixture))

(def directory* (action-fn directory :direct))

(deftest directory*-test
  (is (script-no-comment=
       (stevedore/checked-commands "Directory file1" "mkdir -p file1")
       (binding [pallet.action-plan/*defining-context* nil]
         (-> (directory* {} "file1") first second)))))

(deftest directory-test
  (is (script-no-comment=
       (stevedore/checked-commands "Directory file1" "mkdir -p file1")
       (first (build-actions {} (directory "file1")))))
  (is (script-no-comment=
       (stevedore/checked-commands
        "Directory file1"
        "mkdir -m \"0755\" -p file1"
        "chown --recursive u file1"
        "chgrp --recursive g file1"
        "chmod 0755 file1")
       (first
        (build-actions {}
          (directory "file1" :owner "u" :group "g" :mode "0755")))))
  (testing "non-recursive"
    (is (script-no-comment=
         (stevedore/checked-commands
          "Directory file1"
          "mkdir -m \"0755\" -p file1"
          "chown u file1"
          "chgrp g file1"
          "chmod 0755 file1")
         (first
          (build-actions {}
            (directory
             "file1" :owner "u" :group "g" :mode "0755" :recursive false))))))
  (testing "delete"
    (is (script-no-comment=
         (stevedore/checked-script
          "Delete directory file1"
          "rm --force --recursive file1")
         (first
          (build-actions {}
            (directory "file1" :action :delete :recursive true)))))))

(deftest directories-test
  (is (script-no-comment=
       (str
        (binding [pallet.action-plan/*defining-context* nil]
          (stevedore/chain-commands
           (-> (directory* {} "d1" :owner "o") first second)
           (-> (directory* {} "d2" :owner "o") first second)))
        \newline)
       (first
        (build-actions {}
          (directories ["d1" "d2"] :owner "o"))))))
