;; Run all tests
(load-file "test/common_test.clj")
(load-file "test/utils/response_test.clj")
(load-file "test/utils/encryption_test.clj")
(load-file "test/middleware/auth_test.clj")
(load-file "test/middleware/rate_limit_test.clj")
(load-file "test/view/core_test.clj")
(load-file "test/view/layout_test.clj")
(load-file "test/routes_test.clj")
(load-file "test/integration/middleware_chain_test.clj")
(load-file "test/integration/routes_test.clj")
(load-file "test/integration/middleware_stack_test.clj")
nil