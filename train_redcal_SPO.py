import openke
from openke.config import Trainer, Tester
from openke.module.model import RESCAL
from openke.module.loss import MarginLoss
from openke.module.strategy import NegativeSampling
from openke.data import TrainDataLoader, TestDataLoader

# dataloader for training
train_dataloader = TrainDataLoader(
    in_path="./benchmarks/SPO/",
    nbatches=100,
    threads=8,
    sampling_mode="normal",
    bern_flag=1,
    filter_flag=1,
    neg_ent=25,
    neg_rel=0)

# dataloader for test
test_dataloader = TestDataLoader("./benchmarks/SPO/", "link")

# define the model
transd = RESCAL(
    ent_tot=train_dataloader.get_ent_tot(),
    rel_tot=train_dataloader.get_rel_tot(),
    dim=50)

# define the loss function
model = NegativeSampling(
    model=transd,
    loss=MarginLoss(margin=1.0),
    batch_size=train_dataloader.get_batch_size()
)

# train the model
trainer = Trainer(model=model, data_loader=train_dataloader, train_times=1000, alpha=0.1, use_gpu=True)
trainer.run()
transd.save_checkpoint('./checkpoint/rascal_spo.ckpt')

# test the model
transd.load_checkpoint('./checkpoint/rascal_spo.ckpt')
tester = Tester(model=transd, data_loader=test_dataloader, use_gpu=True)
tester.run_link_prediction(type_constrain=True)
