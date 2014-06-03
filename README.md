Raft Tests
==========

Testing code for raft4ws and comparison with ZooKeeper
Depends on the [raft4ws](https://github.com/filipecampos/raft4ws "Raft4WS") project.

## Release History

* **0.1**: Initial release. (26 May, 2014)

## Installing and using Raft Tests 

Follow the instructions on the INSTALL file to build the binary distribution package.
It will be located at ./target/raft_tests-0.1-bin.zip.
Extract the contents of the zip file and follow the following instructions to execute a Raft server or client.


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
 
 
In order to gather metrics during the tests, you will need to download [Hyperic SIGAR 1.6.4]
(http://sourceforge.net/projects/sigar/files/sigar/1.6/hyperic-sigar-1.6.4.zip/download "Hyperic SIGAR 1.6.4 Zip"), extract it, 
and to copy the contents of sigar-bin/lib to the lib directory under 
the directory resultant from unzipping raft_tests-0.1-bin.zip.  