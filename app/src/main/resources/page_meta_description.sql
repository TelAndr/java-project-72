create table page_meta_description (
  id bigserial primary key,
  url text not null,
  content text not null,
  created_at timestamp not null default now()
);
