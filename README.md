Raft Tests
==========

Testing code for raft4ws and comparison with ZooKeeper
Depends on the raft4ws project (available at https://github.com/filipecampos/raft4ws).

Build files will be uploaded until 30th May 2014.

For Raft tests:
- To run a Raft manager:
 $ ./bin/raft_manager <test_id(e.g.raft)> <total_clients> <total_servers> <iterations> <period(ms)> <1st_kill(ms)> <number_of_leaders_to_kill> <number_of_followers_to_kill> <killing_interval(ms)>

- To run a monitored Raft server:
 $ ./bin/raft_server <server_number> <election_timeout(ms)>

- To run a monitored Raft client:
 $ ./bin/raft_client <client_number> <iterations> <period>
 
 
 For ZooKeeper tests:
- To run a ZooKeeper manager:
 $ ./bin/zoo_manager <test_id(e.g.zookeeper)> <total_clients> <total_servers> <iterations> <period(ms)> <1st_kill(ms)> <ips_to_kill> <killing_interval(ms)>

- To run a ZooKeeper server monitor on the same host running a ZooKeeper server:
 $ ./bin/zoo_server_monitor <zoo_server_pid> <monitored_network_interface>

- To run a monitored ZooKeeper client:
 $ ./bin/zoo_client <client_number> <connection_string> <iterations> <period>