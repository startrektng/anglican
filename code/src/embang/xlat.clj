(ns embang.xlat)

(declare expression)

(defn elist
  "translates a list of expressions,
  replacing define with let"
  [exprs]
  (when (seq exprs)
    (lazy-seq
      (let [[expr & exprs] exprs]
        (if (and (or (seq? expr) (vector? expr))
                 (#{'define 'assume} (first expr)))
          (let [[name value] (rest expr)]
            `((~'let [~name ~(expression value :name name)]
                ~@(elist exprs))))
          `(~(expression expr) 
            ~@(elist exprs)))))))

(defn alambda
  "translates lambda to fn,
  if name is not nil, fn is named"
  [name [parms & body]]
  `(~'fn ~@(when name [name])
     ~(if (list? parms)
        `[~@parms]
        `[& ~parms]) ; variadic
     ~@(elist body)))

(defn amem
  "translates mem, carrying the name to the argument"
  [name [expr]]
  `(~'mem ~(expression expr :name name)))

(defn alet
  "translates let"
  [[bindings & body]]
  `(~'let [~@(mapcat (fn [[name value]]
                     [name (expression value :name name)])
                   bindings)]
     ~@(elist body)))

(defn acond 
  "translates cond"
  [clauses]
  `(~'cond ~@(mapcat (fn [[cnd expr]]
                       [(if (= cnd 'else) :else
                          (expression cnd))
                        (expression expr)])
                     clauses)))

(defn abegin
  "translates begin to do"
  [exprs]
  `(~'do ~@(elist exprs)))

(defn apredict
  "translates predict"
  ;; In Clojure representation `predict' has two arguments:
  ;; symbolic expression and value. This is necessary to
  ;; display predicted expressions in Anglican rather than
  ;; Clojure syntax.
  [[expr]]
  `(~'predict '~expr ~(expression expr)))
        
(defn aform
  "translates compatible forms and function applications"
  [expr]
  (map expression expr))

(defn expression [expr & {:keys [name] :or {name nil}}]
  "translates expression"
  (if (or (seq? expr) (vector? expr))
    (let [[kwd & args] expr]
      (case kwd
        quote  expr
        lambda (alambda name args)
        let    (alet args)
        mem    (amem name args)
        cond   (acond args)
        begin  (abegin args)
        predict (apredict args)
        ;; other forms (if, and, or, application)
        ;;  have compatible structure
        (aform expr)))
    (case expr
      ;; replace variable names `do' and `fn' by `begin' and `lambda',
      ;; to avoid name clashes in Clojure
      do 'begin
      fn 'lambda
      expr)))

(defn program
  "translates anglican program to clojure function"
  [p]
  `(~'fn []
     ~@(elist p)))