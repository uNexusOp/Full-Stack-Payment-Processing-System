CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,

                          user_id BIGINT NOT NULL,

                          idempotency_key VARCHAR(100) NOT NULL UNIQUE,

                          amount NUMERIC(15, 2) NOT NULL,

                          currency VARCHAR(10) NOT NULL DEFAULT 'INR',

                          status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_payment_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id)
);