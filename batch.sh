python3 train_transd_SPO_isA.py > TransD-SPO_isA.log 2>&1
python3 train_transd_KGRC.py > TransD-KGRC.log 2>&1
#
python3 train_redcal_SPO.py > RESCAL-SPO.log 2>&1
python3 train_rescal_SPO-isA.py > RESCAL-SPO_isA.log 2>&1
python3 train_rescal_KGRC.py > RESCAL-KGRC.log 2>&1
#
python3 train_rotate_SPO.py > RotatE-SPO.log 2>&1
python3 train_rotate_SPO-isA.py > RotatE-SPO_isA.log 2>&1
python3 train_rotate_KGRC.py > RotatE-KGRC.log 2>&1

