;; Red Hat Linux default .emacs initialization file

;; don't show annoying splash screen on startup
(setq inhibit-splash-screen t)

;; Are we running XEmacs or Emacs?
(defvar running-xemacs (string-match "XEmacs\\|Lucid" emacs-version))

;; Set up the keyboard so the delete key on both the regular keyboard
;; and the keypad delete the character under the cursor and to the right
;; under X, instead of the default, backspace behavior.
(global-set-key [delete] 'delete-char)
(global-set-key [kp-delete] 'delete-char)

;; Turn on font-lock mode for Emacs
(cond ((not running-xemacs)
       (global-font-lock-mode t)
))
(set-default-font "7x14")

;; Always end a file with a newline
(setq require-final-newline t)

;; Stop at the end of the file, not just add lines
(setq next-line-add-newlines nil)

;; Enable wheelmouse support by default
(if (not running-xemacs)
    (require 'mwheel) ; Emacs
  (mwheel-install) ; XEmacs
)

;;;;;;
;;;
;;; sbk's general look and feel/behavior preferences...
;;;

;; wdired mode for interactive file renaming
(require 'wdired)
(define-key dired-mode-map "r" 'wdired-change-to-wdired-mode)

;; enable auto-compression-mode
(auto-compression-mode t)

;; turn on transient mark mode for color-hilights and regional ops
(transient-mark-mode 1)

;; after emerge nxml-mode (w/env ACCEPT_KEYWORDS="~x86")
;(load "/usr/share/emacs/site-lisp/site-gentoo")

;;(set-background-color "gray10")  ;cyan, SkyBlue4, dark slate gray, dark slate blue, gray10
;;(set-foreground-color "gainsboro")  ;black, thistle1, gainsboro, powder blue
;;(set-cursor-color "gainsboro")
;(set-background-color "white")  ;cyan, SkyBlue4, dark slate gray, dark slate blue, gray10
;(set-foreground-color "gray10")  ;black, thistle1, gainsboro, powder blue
;(set-cursor-color "gray10")

(setq default-frame-alist '((top . 1) (left . 680) (width . 170) (height . 85)))
(setq next-line-add-newlines nil)

(setq inferior-lisp-program "/usr/bin/clisp")

(setq column-number-mode t)
(setq display-time-day-and-date t)
(setq display-time-mode 1)
;;;
;;; ...sbk
;;;
;;;;;;

;(set-language-environment "English")
(set-language-environment "UTF-8")

(defconst my-java-style
	'((c-basic-offset . 2)
	  (c-offsets-alist . ((inline-open . 0) (substatement-open . 0)))
;		(c-auto-newline . t)
	  )
	"My Java Programming Style")
(defun my-c-mode-common-hook ()
	(c-add-style "personal" my-java-style t))
(add-hook 'c-mode-common-hook 'my-c-mode-common-hook)

(custom-set-variables
 ;Insert spaces, not tabs
 ;'(indent-tabs-mode nil)

 ;2 spaces per 'tab'
 '(tab-width 2)
 '(c-basic-offset 2)
 '(delete-selection-mode nil nil (delsel))

 '(jde-jdk-registry (quote (("1.5.0" . "/etc/opt/java"))))
 '(jde-gen-cflow-enable t)
 '(jde-enable-abbrev-mode nil)
 '(tempo-interactive t)
 '(jde-complete-function 'jde-complete-minibuf)
 '(jde-electric-return-p nil)
 '(jde-run-classic-mode-vm t)
 '(jde-debugger '("JDEbug"))
 '(jde-sourcepath
   '("/home/sbk/co/search/src/java"
     "/home/sbk/co/search/src/test/util"
     "/home/sbk/co/search/src/test/unit"
     "/home/sbk/co/reclink/src/java"
     "/home/sbk/co/reclink/src/test/unit"
     "/home/sbk/co/bulk-link/src/java"
     "/home/sbk/co/bulk-link/src/test/unit"))
 '(jde-built-class-path
   '(
     "/home/sbk/co/search-subsys/modules/reclink/target/classes"
     "/home/sbk/co/search-subsys/modules/reclink/target/unit"
;     "/home/sbk/co/search-subsys/modules/reclink/target/contract"
;     "/home/sbk/co/search-subsys/modules/reclink/target/container"
;     "/home/sbk/co/search-subsys/modules/reclink/target/performance"

     "/home/sbk/co/search-subsys/modules/bulk-link/target/classes"
     "/home/sbk/co/search-subsys/modules/bulk-link/target/unit"

     "/home/sbk/co/search-subsys/modules/search/target/classes"
     "/home/sbk/co/search-subsys/modules/search/target/util"
     "/home/sbk/co/search-subsys/modules/search/target/unit"
     "/home/sbk/co/search-subsys/modules/search/target/contract"
;     "/home/sbk/co/search-subsys/modules/search/target/container"
;     "/home/sbk/co/search-subsys/modules/search/target/performance"
     ))
 '(max-lisp-eval-depth 8192)
 '(jde-xref-db-base-directory "/home/sbk/co/search-subsys")
 '(jde-xref-store-prefixes
   '("org.familysearch.search"
     "org.familysearch.reclink"))
 ;; jde annoyingly resets this to nil at times!
 '(case-fold-search t)

 '(scroll-bar-mode (quote right))
 '(speedbar-frame-parameters (quote ((minibuffer) (width . 33) (border-width . 0) (menu-bar-lines . 0) (unsplittable . t))))
 '(speedbar-mode-specific-contents-flag t)
 '(speedbar-sort-tags t))
(custom-set-faces
 '(modeline ((t (:inverse-video nil :foreground "gray10" :background "gainsboro")))))

;;Turn on "font lock" mode to fontify (highlight) programming buffers
(global-font-lock-mode 1)

;;Custom key bindings
;; (see node "Emacs:Customization:Key Bindings:Init Rebinding" in "Info")
(global-set-key "\C-cc" 'clipboard-kill-ring-save)  ;C-c c  <=>  copy
(global-set-key "\C-cb" 'compile)          ;C-c b  <=>  compile (build)
(global-set-key "\C-cs" 'shell)            ;C-c s  <=>  M-x shell
(global-set-key [end] 'end-of-line)        ;<END>  <=>  C-e
(global-set-key [home] 'beginning-of-line) ;<HOME> <=>  C-a
(global-set-key [f7] 'compile)             ;<F7>   <=>  M-x compile
(global-set-key "\C-cC" 'clipboard-kill-ring-save)  ;C-c C <=> copy
(global-set-key "\C-cl" 'goto-line)        ;C-c l  <=> goto-line
(global-set-key "\C-cq" 'query-replace-regexp) ;C-c q <=> query-replace-regexp
(global-set-key "\C-cr" 'replace-string)   ;C-c r  <=> replace-string
(global-set-key "\C-cf" 'find-grep-dired)  ;C-c g  <=> find-grep-dired
(global-set-key "\C-c\C-f" 'find-name-dired)  ;C-c C-F  <=> find-name-dired
(global-set-key "\C-cF" 'grep-find)        ;C-c F  <=> grep-find
(global-set-key "\C-cg" 'grep)             ;C-c g  <=> grep
(global-set-key "\C-cm" 'count-matches)    ;C-c m  <=> count-matches
(global-set-key "\C-ck" 'global-set-key)   ;C-c k  <=> global-set-key
(global-set-key [C-home] 'beginning-of-buffer)
(global-set-key [C-end] 'end-of-buffer)
(global-set-key "\C-cd" 'speedbar)
(global-set-key "\C-c\C-d" 'ediff-buffers) ;C-c C-d <=> ediff-buffers
(global-set-key "\C-c\C-i" 'indent-region)
(global-set-key "\C-cu" 'rename-uniquely)
(global-set-key "\C-c\C-l" 'run-lisp)
(global-set-key "\C-cB" 'browse-url-lynx-emacs)
(global-set-key "\C-c\C-j" 'show-only-java)

;; open a new window instead of using a random window
(setq browse-url-new-window-flag t)

(setq find-function-source-path
  '(
    "/usr/share/emacs/21.3/lisp"
    "/usr/share/emacs/21.3/lisp-src"
    ))

;;todo: get this from the environment!
(setq author "Spence Koehler")

(defun add-javadoc-comment (&optional arg)
	"Inserts javadoc comments at the current cursor position."
	(interactive "P")
;	(push-mark)
	(let ((pre-macro [?\C-i ?/ ?* ?* ?\C-m ?* ?\C-m ?* ?  ?< ?p ?> ?\C-m ?* ?  ?@ ?a ?u ?t ?h ?o ?r ?  ])
        (post-macro [?\C-m ?* ?/ ?\C-m ?\C-i]))
		(execute-kbd-macro (vconcat pre-macro author post-macro))))

(defun show-only-java (&optional arg)
  "Deletes lines that don't end in .java"
  (interactive "P")
  (let ((macro
         [?\C-  ?\C-s ?. ?j ?a ?v ?a ?\C-q ?\C-j ?\C-a ?\C-p ?\C-a ?\C-w ?\C-n ?\C-a])
        (count (if (numberp arg) arg 10000)))
    (dotimes (i count)
      (execute-kbd-macro macro))
;    (unless arg
;      (execute-kbd-macro [?\C-  ?\M-> ?\C-w]))
    ))


(global-set-key "\C-cj" 'add-javadoc-comment)

(defun add-braces (&optional arg)
	"Inserts a pair of braces at the current cursor position."
	(interactive "P")
	(let ((macro
				 [?\C-e ?  ?{ ?\C-m ?} ?\C-a ?\C-o ?\C-i]))
		(execute-kbd-macro macro)))

(global-set-key "\C-ci" 'add-braces)

(defun filepath2classpath (filepath)
  (let*
      ((sub0 (replace-regexp-in-string "/" "." filepath))
       (compos (string-match "\\.com\\." sub0))
       (orgpos (string-match "\\.org\\." sub0))
       (netpos (string-match "\\.net\\." sub0))
       (javapos (string-match "\\.java\\." sub0))
       (thepos (if (null compos) (if (null orgpos) (if (null netpos) (+ javapos 5) netpos) orgpos) compos))
       (sub1 (substring sub0 (1+ thepos)))
       (sub2 (replace-regexp-in-string ".java" "" sub1)))
  sub2))

(defun filepath2projectdir (filepath)
	(let ((srcpos (string-match "/src/" filepath)))
		(substring filepath 0 srcpos)))

(defun position-from-end (char string)
  (let ((result nil)
        (i (length string)))
    (while (and (null result) (> i 0))
      (setq i (1- i))
      (if (eq (aref string i) char)
        (setq result i)))
    result))

(defun classpath2package (classpath)
  (let ((ppos (position-from-end ?. classpath)))
    (if ppos (substring classpath 0 (position-from-end ?. classpath)) classpath)))

(defun add-package (&optional arg)
	"Inserts a 'package' line at the top of the java file.
  NOTE: It is assumed that the package is named beginning with an 'com.' directory."
	(interactive "P")
	(let ((preMacro [?\M-< ?\C-o]))
		(execute-kbd-macro preMacro)
    (insert "package ")
    (insert (classpath2package (filepath2classpath buffer-file-name)))
    (insert ";\n\n")
    ))

(global-set-key "\C-cp" 'add-package)

(defun java-class-skeleton (&optional arg)
	"Creates a java class skeleton in the current buffer (named *.java)"
	(interactive "P")
	(let ((path (split-string buffer-file-name "/")))
		(insert "public class ")

    ; insert the last element on the path (filename) minus the ".java" part
		(insert (substring (car (last path)) 0 -5))

		(add-braces)
		(execute-kbd-macro [?\C-a ?\C-p])
		(add-javadoc-comment)
		(add-package)
;		(insert "\n\nimport java.util.*;\nimport java.io.*;")
		(execute-kbd-macro [?\C-n ?\C-n ?\C-e ? ])
		))

(global-set-key "\C-cJ" 'java-class-skeleton)

;(defun copy-to-end-of-line (&optional arg)
;  (interactive "P")
;  (execute-kbd-macro [?\C-  ?\C-n ?\C-c ?c ?\C-x ?\C-x ?\C-g]))
;
;(global-set-key "\C-cy" 'copy-to-end-of-line)

(defun execute-java-file (&optional arg)
  (interactive "P")
  (let (
        (command "java -Xmx640m ")
        (cp1 "-cp `cpgen ")
				(cp2 (filepath2projectdir buffer-file-name))
				(cp3 "` ")
        (test-command "junit.textui.TestRunner ")
;        (command "j ")
;        (test-command "jt ")
        (javaclass (filepath2classpath buffer-file-name)))
    (execute-kbd-macro [?\C-c ?s ?\M->])
		(insert command)
		(insert cp1) (insert cp2) (insert cp3)
    (if (string-match ".Test" javaclass) (insert test-command))
    (insert javaclass)
    ))

(global-set-key "\C-ce" 'execute-java-file)

;;;
;;; Insert "import" statements for java classes. (control-c i)
;;;
(defun import-classes (&optional arg)
  "Finds possible classes for the word at the current point, adding an 'import'
   statement to the java file (interactively if more than one to choose)."
  (interactive "P")
  (let* ((name (thing-at-point 'word))
         (paths-string
          ; note: relies on shell command "cpfinder name"
          (shell-command-to-string
           (concat "cpfinder " name)))
         (len (length paths-string))
         path)

    (when (> len 0)
      ; return to point where we started when done
      (save-excursion

        ; position point at end of imports or under "package"
        (unless (re-search-backward "^import " nil t)
                                        ;go to beginning of buffer and down
          (goto-char (point-min))
          (forward-line 1)
          )
        (forward-line 1)

        ; deal with possibilities
        (let ((paths (split-string paths-string "\n")))
          (setq
           path
           (if (= (length paths) 1)
                                        ; only 1: don't need to ask
               (insert-import paths "0")
                                        ; more than 1: create prompt with choices and ask
             (let ((prompt "") choice (count 0))
               (dolist (path paths)
                 (when (> (length path) 0)
                   (setq prompt (concat prompt "(" (number-to-string count) ") " path "\n"))
                   (setq count (+ count 1))))
               (setq prompt (concat prompt "choice [0-" (number-to-string (- count 1)) "]: "))
               (setq choice (read-no-blanks-input prompt))
               (insert-import paths choice))))
          ))
;      (ding)
      (momentary-string-display "" (point) nil (concat "imported " path))
      )))

(defun insert-import (paths choice)
  "auxiliary to import-classes for inserting the 'import' statement"
  (let* ((pos (string-to-number choice))
         (path (elt paths pos))
         (import (concat "import " path ";")))
    ;todo: don't import when in same package?
    ; only import if not already there
    (unless (re-search-backward (concat "^" import) nil t)
      (insert import)
      (insert "\n"))
    path))

(global-set-key "\C-ci" 'import-classes)
;; end of import-classes

(add-hook 'c-mode-hook
  (function
	 (lambda ()
		 (unless (or (file-exists-p "makefile")
								 (file-exists-p "Makefile"))
			 (make-local-variable 'compile-command)
			 (setq compile-command
						 (concat "make -k "
										 (file-name-sans-extension buffer-file-name)))))))

(add-hook 'java-mode-hook
	(function
	 (lambda()
		 (make-local-variable 'compile-command)
		 (setq compile-command (concat "buildone " buffer-file-name)))))

(add-hook 'nxml-mode-hook
  (function
   (lambda()
     (make-local-variable 'compile-command)
     (setq compile-command (concat "buildcopy " buffer-file-name)))))

(add-hook 'text-mode-hook
  (function
   (lambda()
     (make-local-variable 'compile-command)
     (setq compile-command (concat "buildcopy " buffer-file-name)))))

;;
;; define (.txt, .csv) files as "text" mode so that compile hook can be added.
;;
(setq auto-mode-alist
  (append
   '((".*\\.txt\\'" . text-mode)
     (".*\\.out\\'" . text-mode)
     (".*\\.gz\\'" . text-mode)
     (".*\\.def\\'" . text-mode)
     (".*\\.fcm\\'" . text-mode)
     (".*\\.osob\\'" . text-mode)
     (".*\\.vm\\'" . text-mode)
     (".*\\.xinc\\'" . text-mode)
     (".*\\.attributes\\'" . text-mode)
     (".*\\.properties\\'" . text-mode)
     (".*\\.arff\\'" . text-mode)
     (".*\\.csv\\'" . text-mode))
   auto-mode-alist))

(setq visible-bell t)   ;; instead of audible bell

(put 'downcase-region 'disabled nil)
(put 'upcase-region 'disabled nil)

;;todo: get this from the environment!
(setq sandbox-name "co")

(setq sandbox-name-as-path (concat "/" sandbox-name "/"))

;; find project root
(defun find-project-root (filename &optional arg)
  "Function to find the project root for the named file."
  (let* ((sandbox-name-pos (string-match sandbox-name-as-path filename))
         (next-slash-pos (string-match "/" filename (+ sandbox-name-pos (length sandbox-name-as-path)))))
    (substring filename 0 next-slash-pos))
  )

;; goto-file macro
(defun find-source (&optional arg)
  "Macro to find the source files under the project root of the class at point."
  (interactive "P")
  (let ((pre-macro [?\M-b ?\C-  ?\M-f ?\C-c ?c])
        (project-root (find-project-root buffer-file-name))
        (post-macro [?\C-c ?\C-f ?\C-m ?\C-y ?. ?j ?a ?v ?a ?\C-m]))
    (execute-kbd-macro pre-macro)
    (find-file-other-window project-root)
    (execute-kbd-macro post-macro)))
;(fset 'find-source
;   '[?\M-b ?\C-  ?\M-f ?\C-c ?c ?\C-x ?4 ?f ?~ ?/ ?c ?o ?/ ?s ?e ?a ?r ?c ?h ?- ?s ?u ?b ?s ?y ?s ?\C-m ?\C-c ?\C-f ?\C-m ?\C-y ?. ?j ?a ?v ?a ?\C-m])
(global-set-key "\C-cG" 'find-source)             ;C-c G  <=> goto file

;; find-usages macro
(defun find-usages (&optional arg)
  "Macro to find usages of the current symbol at point."
  (interactive "P")
  (let ((pre-macro [?\M-b ?\C-  ?\M-f ?\C-c ?c])
        (project-root (find-project-root buffer-file-name))
        (post-macro [?\C-c ?f ?\C-m ?\C-y ?\C-m]))
    (execute-kbd-macro pre-macro)
    (find-file-other-window project-root)
    (execute-kbd-macro post-macro)))
;(fset 'find-usages
;   '[?\M-b ?\C-  ?\M-f ?\C-c ?c ?\C-x ?4 ?f ?~ ?/ ?c ?o ?/ ?s ?e ?a ?r ?c ?h ?- ?s ?u ?b ?s ?y ?s ?\C-m ?\C-c ?f ?\C-m ?\C-y ?\C-m])
(global-set-key "\C-cU" 'find-usages)

;; Start emacs server for emacs client to connect to
;; use "C-x #" to send the buffer.
;note: can't use emacs server and gnuserver at the same time!
;(server-start)

;; JDEE
;(load "$US_HOME/modules/tools/emacs/jmacs.el")

;; JDE minimul setup

;; Set the debug option to enable a backtrace when a
;; problem occurs.
;(setq debug-on-error t)

;; Update the Emacs load-path to include the path to
;; the JDE and its require packages.
;(add-to-list 'load-path (expand-file-name "/opt/emacs/jde/lisp"))
;(add-to-list 'load-path (expand-file-name "/opt/emacs/cedet/common"))
;(add-to-list 'load-path (expand-file-name "/opt/emacs/elib"))

;; Initialize CEDET.
;(load-file (expand-file-name "/opt/emacs/cedet/common/cedet.el"))

; don't check version of CEDET
;(setq jde-check-version-flag nil)

;; If you want Emacs to defer loading the JDE until you open a 
;; Java file, edit the following line
;(setq defer-loading-jde nil)
;; to read:
;;
;;  (setq defer-loading-jde t)
;;

;(if defer-loading-jde
;    (progn
;      (autoload 'jde-mode "jde" "JDE mode." t)
;      (setq auto-mode-alist
;	    (append
;	     '(("\\.java\\'" . jde-mode))
;	     auto-mode-alist)))
;  (require 'jde))
;
;
;;; Sets the basic indentation for Java source files
;;; to two spaces.
;(defun my-jde-mode-hook ()
;  (setq c-basic-offset 2))
;
;(add-hook 'jde-mode-hook 'my-jde-mode-hook)
;
;;; bind Ctrl-c B to describing call tree
;(global-set-key "\C-cB" 'jde-xref-display-call-tree)
;;; bind Ctrl-c U to updating the xref db
;;(global-set-key "\C-cU" 'jde-xref-update)
;;; bind Ctrl-c M to (re)making the xref db
;(global-set-key "\C-cM" 'jde-xref-make-xref-db)


;; Include the following only if you want to run
;; bash as your shell.

;; Setup Emacs to run bash as its primary shell.
(setq shell-file-name "bash")
(setq shell-command-switch "-c")
(setq explicit-shell-file-name shell-file-name)
(setenv "SHELL" shell-file-name)
(setq explicit-sh-args '("-login" "-i"))


;;
;; include gnuserv for integration with eclipse
;;
;(require 'gnuserv-compat)
;(require 'gnuserv)
;(gnuserv-start)
;; if XEmacs gnuserv binary gets in the way:
;; (setq server-program "/opt/emacs/gnuserv/default/gnuserv")
;(setq gnuserv-frame (selected-frame))


;;--------------------------------------------------------------------
;; Lines enabling gnuplot-mode

;; move the files gnuplot.el to someplace in your lisp load-path or
;; use a line like
;;  (setq load-path (append (list "/path/to/gnuplot") load-path))

;; these lines enable the use of gnuplot mode
  (autoload 'gnuplot-mode "gnuplot" "gnuplot major mode" t)
  (autoload 'gnuplot-make-buffer "gnuplot" "open a buffer in gnuplot mode" t)

;; this line automatically causes all files with the .gp extension to
;; be loaded into gnuplot mode
  (setq auto-mode-alist (append '(("\\.gp$" . gnuplot-mode)) auto-mode-alist))

;; This line binds the function-9 key so that it opens a buffer into
;; gnuplot mode 
  (global-set-key [(f9)] 'gnuplot-make-buffer)

;; end of line for gnuplot-mode
;;--------------------------------------------------------------------

;(setq load-path (cons "/home/user/somewhere/emacs" load-path))
;(if (not (string-match "XEmacs" emacs-version))
;  (progn
;    (require 'unicode)
;    ;(setq unicode-data-path "..../UnicodeData-3.0.0.txt")
;    (if (eq window-system 'x)
;      (progn
;        (setq fontset12
;          (create-fontset-from-fontset-spec
;            "-misc-fixed-medium-r-normal-*-12-*-*-*-*-*-fontset-standard"))
;        (setq fontset13
;          (create-fontset-from-fontset-spec
;            "-misc-fixed-medium-r-normal-*-13-*-*-*-*-*-fontset-standard"))
;        (setq fontset14
;          (create-fontset-from-fontset-spec
;            "-misc-fixed-medium-r-normal-*-14-*-*-*-*-*-fontset-standard"))
;        (setq fontset15
;          (create-fontset-from-fontset-spec
;            "-misc-fixed-medium-r-normal-*-15-*-*-*-*-*-fontset-standard"))
;        (setq fontset16
;          (create-fontset-from-fontset-spec
;            "-misc-fixed-medium-r-normal-*-16-*-*-*-*-*-fontset-standard"))
;        (setq fontset18
;          (create-fontset-from-fontset-spec
;            "-misc-fixed-medium-r-normal-*-18-*-*-*-*-*-fontset-standard"))
;       ; (set-default-font fontset15)
;        ))))
;
;;(setq load-path (cons "/home/user/somewhere/emacs" load-path))
;(if (not (string-match "XEmacs" emacs-version))
;  (progn
;    (require 'oc-unicode)
;    ;(setq unicode-data-path "..../UnicodeData-3.0.0.txt")
;    (if (eq window-system 'x)
;      (progn
;        (setq fontset12
;          (oc-create-fontset
;            "-misc-fixed-medium-r-normal-*-12-*-*-*-*-*-fontset-standard"
;            "-misc-fixed-medium-r-normal-ja-12-*-iso10646-*"))
;        (setq fontset13
;          (oc-create-fontset
;            "-misc-fixed-medium-r-normal-*-13-*-*-*-*-*-fontset-standard"
;            "-misc-fixed-medium-r-normal-ja-13-*-iso10646-*"))
;        (setq fontset14
;          (oc-create-fontset
;            "-misc-fixed-medium-r-normal-*-14-*-*-*-*-*-fontset-standard"
;            "-misc-fixed-medium-r-normal-ja-14-*-iso10646-*"))
;        (setq fontset15
;          (oc-create-fontset
;            "-misc-fixed-medium-r-normal-*-15-*-*-*-*-*-fontset-standard"
;            "-misc-fixed-medium-r-normal-ja-15-*-iso10646-*"))
;        (setq fontset16
;          (oc-create-fontset
;            "-misc-fixed-medium-r-normal-*-16-*-*-*-*-*-fontset-standard"
;            "-misc-fixed-medium-r-normal-ja-16-*-iso10646-*"))
;        (setq fontset18
;          (oc-create-fontset
;            "-misc-fixed-medium-r-normal-*-18-*-*-*-*-*-fontset-standard"
;            "-misc-fixed-medium-r-normal-ja-18-*-iso10646-*"))
;       ; (set-default-font fontset15)
;        ))))
;
;


;;;;;;
;;;
;;; Initialize buffers
;;;

;(find-file "co/core")
;(split-window-vertically)
;(other-window 1)
;(shell)
;(other-window 1)
;;(find-name-dired "src" "sd")
;(find-file "src/java/org.sd.)
;(find-file "../../../test/unit/org.sd.)
;(switch-to-buffer "sd")
