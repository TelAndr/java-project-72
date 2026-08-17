create table page_h1 (
  id bigserial primary key,
  url text not null,
  h1_text text not null,
  created_at timestamp not null default now()
);
