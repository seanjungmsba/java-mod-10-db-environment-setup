control 'Postgres Master Running' do
  impact 'critical'
  title 'Postgres Master Node is running'
  desc 'The Postgres RepMgr image is pulled, and a Master instance is running'

  describe docker.images.where { repository == 'bitnami/postgresql-repmgr' && tag == '14.4.0-debian-11-r9' } do
    it { should exist }
  end
  describe docker.containers.where { names == 'postgres-master-1' && image == 'bitnami/postgresql-repmgr:14.4.0-debian-11-r9' && ports =~ /0.0.0.0:5432/ } do
    its('status') { should match [/Up/] }
  end
  describe postgres_session('postgres', 'mysecretpassword', 'postgres-master-1', 5432).query('SELECT datname FROM pg_database;') do
    its('output') { should include 'db_test' }
  end
end

control 'Postgres Replica 1 Running' do
  impact 'critical'
  title 'Postgres Replica Node 1 is running'
  desc 'A Postgres Replica instance is running'

  describe docker.containers.where { names == 'postgres-replica-1' && image == 'bitnami/postgresql-repmgr:14.4.0-debian-11-r9' && ports =~ /0.0.0.0:5433/ } do
    its('status') { should match [/Up/] }
  end
  describe postgres_session('postgres', 'mysecretpassword', 'postgres-replica-1', 5432).query('SELECT datname FROM pg_database;') do
    its('output') { should include 'db_test' }
  end
end

control 'Postgres Counter Table' do
  impact 'critical'
  title 'Counter Table exists'
  desc 'A db_test.counter table exists in the cluster'

  describe postgres_session('postgres', 'mysecretpassword', 'postgres-master-1', 5432).query('SELECT tablename FROM pg_tables;', ['db_test']) do
    its('output') { should include 'counter' }
  end
end

control 'Postgres Replica 2 Running' do
  impact 'critical'
  title 'Postgres Replica Node 2 is running'
  desc 'A Postgres Replica instance is running'

  describe docker.containers.where { names == 'postgres-replica-2' && image == 'bitnami/postgresql-repmgr:14.4.0-debian-11-r9' && ports =~ /0.0.0.0:5434/ } do
    its('status') { should match [/Up/] }
  end
  describe postgres_session('postgres', 'mysecretpassword', 'postgres-replica-2', 5432).query('SELECT datname FROM pg_database;') do
    its('output') { should include 'db_test' }
  end
end

control 'Postgres Replica 3 Running' do
  impact 'critical'
  title 'Postgres Replica Node 3 is running'
  desc 'A Postgres Replica instance is running'

  describe docker.containers.where { names == 'postgres-replica-3' && image == 'bitnami/postgresql-repmgr:14.4.0-debian-11-r9' && ports =~ /0.0.0.0:5435/ } do
    its('status') { should match [/Up/] }
  end
  describe postgres_session('postgres', 'mysecretpassword', 'postgres-replica-3', 5432).query('SELECT datname FROM pg_database;') do
    its('output') { should include 'db_test' }
  end
end

control 'PgPoolII Running' do
  impact 'critical'
  title 'PgPoolII is running'
  desc 'The PgPoolII instance is pulled and running'

  describe docker.images.where { repository == 'bitnami/pgpool' && tag == '4.3.2-debian-11-r16' } do
    it { should exist }
  end
  describe docker.containers.where { names == 'pgpool' && image == 'bitnami/pgpool:4.3.2-debian-11-r16' && ports =~ /0.0.0.0:5436/ } do
    its('status') { should match [/Up/] }
  end
  describe postgres_session('postgres', 'mysecretpassword', 'pgpool', 5432).query('SELECT datname FROM pg_database;') do
    its('output') { should include 'db_test' }
  end
end

control 'PgPoolII Nodes Connected' do
  impact 'critical'
  title 'PgPoolII Backend Nodes Online'
  desc 'The PgPoolII instance is running, and all the backend nodes are detected'

  describe postgres_session('postgres', 'mysecretpassword', 'pgpool', 5432).query('SHOW POOL_NODES;') do
    its('output') { should match('postgres-master-1').and match('postgres-replica-1').and match('postgres-replica-2').and match('postgres-replica-3') }
  end
end
