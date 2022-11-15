import openke
from openke.config import Trainer, Tester
from openke.module.model import RotatE
from openke.module.loss import SigmoidLoss
from openke.module.strategy import NegativeSampling
from openke.data import TrainDataLoader, TestDataLoader

# dataloader for training
train_dataloader = TrainDataLoader(
    in_path="./benchmarks/SPO-isA/",
    nbatches=100,
    threads=8,
    sampling_mode="normal",
    bern_flag=1,
    filter_flag=1,
    neg_ent=25,
    neg_rel=0)

# dataloader for test
test_dataloader = TestDataLoader("./benchmarks/SPO-isA/", "link")

# define the model
rescal = RotatE(
    ent_tot=train_dataloader.get_ent_tot(),
    rel_tot=train_dataloader.get_rel_tot(),
    dim=1024,
    margin=6.0,
    epsilon=2.0,
)

# define the loss function
model = NegativeSampling(
    model=rescal,
    loss=SigmoidLoss(adv_temperature=2),
    batch_size=train_dataloader.get_batch_size(),
    regul_rate=0.0
)

# train the model
trainer = Trainer(model=model, data_loader=train_dataloader, train_times=1000, alpha=2e-5, opt_method="adam",
                  use_gpu=False)
trainer.run()
transd.save_checkpoint('./checkpoint/rotate_spo_isA.ckpt')

# test the model
transd.load_checkpoint('./checkpoint/rorate_spo_isA.ckpt')
tester = Tester(model=transd, data_loader=test_dataloader, use_gpu=False)
tester.run_link_prediction(type_constrain=True)
