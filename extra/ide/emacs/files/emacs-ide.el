;;;;;;
;;;
;;; Style and functions for using emacs as an ide for java development.
;;;
;;; Used in conjunction with bash environment settings and scripts, this el
;;; file adds very light-weight ide functionality for java development.
;;;
;;; "Light-weight" means that it does not require any special caches while
;;; still providing some ide helper capabilities. Examples of included
;;; light-weight functionality are:
;;;   - WYSIWYG editing
;;;   - Syntax highlighting
;;;   - Compile single classes
;;;   - Compile full projects
;;;   - Auto jump to source-code lines
;;;   - Execute java classes
;;;   - Debug java classes
;;;   - Find course code for classes
;;;   - Find source code for method usages
;;;   - Add import statements for referenced classes
;;;
;;; Heavy-weight functionality that requires special caches and associated
;;; startup and synchronization slowdowns are omitted from this package. For
;;; example, this lacks dynamic function and argument completion.
;;;
;;; Include this file from .emacs like: (i.e. if emacs-ide.el is in the same dir)
;;; (setq load-path (cons "." load-path))
;;; (load "emacs-ide.el")
;;;
;;; author: Spence Koehler (KoehlerSB747@gmail.com)
;;;

;;
;; java style (change to suit your own environment and preferences)
;;
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
'(indent-tabs-mode nil)

 ;2 spaces per 'tab'
 '(tab-width 2)
 '(c-basic-offset 2)
 '(delete-selection-mode nil nil (delsel))

 ;Do case-insensitive find-grepping
 '(find-grep-options "-q -i")

 '(tempo-interactive t)
 '(max-lisp-eval-depth 8192)
 '(case-fold-search t)

 '(scroll-bar-mode (quote right))
 '(speedbar-frame-parameters (quote ((minibuffer) (width . 33) (border-width . 0) (menu-bar-lines . 0) (unsplittable . t))))
 '(speedbar-mode-specific-contents-flag t)
 '(speedbar-sort-tags t))

(custom-set-faces
 '(modeline ((t (:inverse-video nil :foreground "gray10" :background "gainsboro")))))

;;Turn on "font lock" mode to fontify (highlight) programming buffers
(global-font-lock-mode 1)

;;
;; Custom key bindings and ide functions
;;  (see node "Emacs:Customization:Key Bindings:Init Rebinding" in "Info")
;;
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
(setq author (getenv "AUTHOR"))

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
       (junitpos (string-match "\\.junit\\." sub0))
       (unitpos (string-match "\\.unit\\." sub0))
       (testpos (string-match "\\.test\\." sub0))
       (thepos (if (null compos)
									 (if (null orgpos)
											 (if (null netpos)
													 (if (null javapos)
															 (if (null junitpos)
                                   (if (null unitpos)
  																	 (if (null testpos)
	  																		 null
		  																	 (+ testpos 5))
                                     (+ unitpos 5))
			 													 (+ junitpos 6))
														 (+ javapos 5))
												 netpos)
										 orgpos)
								 compos))
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
        (cp1 "-classpath `cpgen ")
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
           (concat "cpfinder " name " " buffer-file-name)))
         (len (length paths-string))
         path)

;debugging:
;(with-output-to-temp-buffer "foo"
;  (print name)
;  (print paths-string)
;  (print len)
;)

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
        (let* ((pieces (split-string paths-string "\n"))  ; split on newline
               (paths (delete-dups pieces)))  ; delete duplicates
          (delete "" paths)                   ; remove empty strings
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

(setq compile-command "buildtarget ")

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
     (".*\\.xml\\'" . text-mode)
     (".*\\.rtf\\'" . text-mode)
     (".*\\.csv\\'" . text-mode))
   auto-mode-alist))

(setq visible-bell t)   ;; instead of audible bell

(put 'downcase-region 'disabled nil)
(put 'upcase-region 'disabled nil)

;;todo: get this from the environment!
;(setq sandbox-name "co")
(setq sandbox-name (getenv "SANDBOX_NAME"))

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

;;
;; Setup Emacs to run bash as its primary shell.
;;
(setq shell-file-name "bash")
(setq shell-command-switch "-c")
(setq explicit-shell-file-name shell-file-name)
(setenv "SHELL" shell-file-name)
(setq explicit-sh-args '("-login" "-i"))

