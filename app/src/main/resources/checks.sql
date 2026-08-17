create table checks (
  id bigserial primary key,
  url_id bigint not null references urls(id),
  type text not null,
  remote_response text not null,
  created_at timestamp default current_timestamp
);