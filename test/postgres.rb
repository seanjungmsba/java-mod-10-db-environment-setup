control 'RepMgr Nodes Connected' do
  impact 'critical'
  title 'RepMgr Backend Nodes Online'
  desc 'RepMgr is working correctly, with all nodes added into the cluster definition'

  describe command('/opt/bitnami/scripts/postgresql-repmgr/entrypoint.sh repmgr -f /opt/bitnami/repmgr/conf/repmgr.conf cluster show') do
    its('stdout') { should match('postgres-master-1').and match('postgres-replica-1').and match('postgres-replica-2').and match('postgres-replica-3') }
  end
end
