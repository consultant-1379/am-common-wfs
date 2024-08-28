#!/bin/sh -f

set -x

gsh modify_ne -ni p4-pcc-1 -mgi 1 -mc 4 -ari 1 -asi 4 -ap 1

gsh create_imsins -imsi 24080 -rs home -dn mnc080.mcc240.gprs -np e214 -ac true -na international -rd 5 -ad 86582 -m1 NULL -m2 NULL -m3 NULL -eplp NULL -rn ericsson.se -hn esm -s8 gtp -gcc NULL -wcc NULL -lcc NULL -rfsp NULL -arph 1 -arpm 2 -gss gr -vlbo NULL -aifv true -mavs 1 -mavr 2 -earplr 0 -st true -sra noaction -ssc NULL -iess NULL -pcr false -sps8 NULL -ivss NULL -sdssc none -qpmll NULL -srgr true -uut NULL -nia denied -drcl 0 -urcl 0 -vlbaoi NULL -iri NULL -crs allowed_cs_service -arpnl NULL -qpmw 1 -qpmg 1 -mps NULL
gsh create_plmn -mcc 240 -mnc 80 -pn 24080 -fnn NULL -snn NULL -ci false -me false -s5 gtp -local true -voip no -sl true -nbt NULL -urcl 0 -drcl 0 -tr NULL -ecvh true -gswgli off
gsh create_inbound_pf_policy -ifp ETH_1_1_1_0_Default
gsh create_inbound_pf_rule -ifp ETH_1_1_1_0_Default -fr 1 -r permit -p ip -d NULL -dm NULL -sip 0.0.0.0 -sipm 0.0.0.0 -dip 0.0.0.0 -dipm 0.0.0.0 -pp 0 -ppq gt -ipo true -sp 0 -spq gt -dp 0 -dpq gt -tf NULL -tfm NULL -it 0 -itq gt -sd NULL -lt false

gsh create_ip_interface -ifn ETH_1_1_1_0_Default -ip 21.30.1.21 -mask 255.255.255.0 -rip NULL -eqp 1.1 -ep 1 -vid 0 -ifp ETH_1_1_1_0_Default -nw Default
gsh create_router_instance -eqp 1.1 -nw Default -ip NULL -sn NULL -sc NULL -sl NULL
gsh create_static_route -eqp 1.1 -nw Default -dip 0.0.0.0 -mask 0.0.0.0 -gip 21.30.1.254

/tmp/DPE_SC/LoadUnits/ttx/int/bin/set_sysprop pcf_available false

# Static GW selection
gsh modify_node_function -name static_gw_selection -state on
gsh create_gw_selection_sgw -sgw p4-pcc-1-sgw -sgwv4 10.81.41.139 -sgwv6 NULL -gcap 100 -gan GA1
gsh create_gw_selection_pgw -pgw p4-pcc-1-pgw -pgwv4 10.81.41.134 -pgwv6 NULL -gcap 100 -gwt SGW_COMBINED -pt GTPV2 -sgw p4-pcc-1-sgw

gsh modify_feature_state -fsi mme_pool -fs ACTIVATED
gsh modify_node_function -name dns_prior_to_static_gw_selection -state on

gsh create_dns_server_address -dn epc.mnc080.mcc240.3gppnetwork.org. -ns dallas.ericsson.se. -ip 10.81.112.49