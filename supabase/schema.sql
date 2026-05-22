-- Run this in your Supabase SQL editor

-- Profiles table (auto-created on sign-up via trigger)
create table if not exists profiles (
  id uuid primary key references auth.users on delete cascade,
  username text unique not null default 'athlete_' || substr(gen_random_uuid()::text, 1, 6),
  avatar_url text,
  is_premium boolean not null default false,
  streak integer not null default 0,
  created_at timestamptz not null default now()
);

-- Sessions table
create table if not exists sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references profiles(id) on delete cascade,
  exercise text not null,
  result jsonb not null,
  created_at timestamptz not null default now()
);

-- Leaderboard view (materialised weekly)
create table if not exists leaderboard (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references profiles(id) on delete cascade,
  exercise text not null,
  avg_score numeric(5,2) not null default 0,
  total_reps integer not null default 0,
  updated_at timestamptz not null default now(),
  unique(user_id, exercise)
);

-- Trigger: upsert leaderboard after each session insert
create or replace function update_leaderboard()
returns trigger language plpgsql as $$
declare
  _reps int := jsonb_array_length(NEW.result->'reps');
  _score numeric := (NEW.result->>'avgScore')::numeric;
begin
  insert into leaderboard (user_id, exercise, avg_score, total_reps, updated_at)
  values (NEW.user_id, NEW.exercise, _score, _reps, now())
  on conflict (user_id, exercise)
  do update set
    avg_score = (leaderboard.avg_score * leaderboard.total_reps + excluded.avg_score * excluded.total_reps)
                / nullif(leaderboard.total_reps + excluded.total_reps, 0),
    total_reps = leaderboard.total_reps + excluded.total_reps,
    updated_at = now();
  return NEW;
end;
$$;

drop trigger if exists on_session_insert on sessions;
create trigger on_session_insert
  after insert on sessions
  for each row execute function update_leaderboard();

-- Trigger: create profile on new user
create or replace function handle_new_user()
returns trigger language plpgsql security definer as $$
begin
  insert into profiles (id)
  values (NEW.id)
  on conflict do nothing;
  return NEW;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

-- RLS policies
alter table profiles enable row level security;
alter table sessions enable row level security;
alter table leaderboard enable row level security;

create policy "Users can read own profile"
  on profiles for select using (auth.uid() = id);

create policy "Users can update own profile"
  on profiles for update using (auth.uid() = id);

create policy "Users can insert own sessions"
  on sessions for insert with check (auth.uid() = user_id);

create policy "Users can read own sessions"
  on sessions for select using (auth.uid() = user_id);

create policy "Anyone can read leaderboard"
  on leaderboard for select using (true);
