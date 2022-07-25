import openke
from openke.config import Trainer, Tester
from openke.module.model import ComplEx
from openke.module.loss import SoftplusLoss
from openke.module.strategy import NegativeSampling
from openke.data import TrainDataLoader, TestDataLoader

# dataloader for training
train_dataloader = TrainDataLoader(
	in_path = "./benchmarks/KGRC/", 
	nbatches = 100,
	threads = 8, 
	sampling_mode = "normal", 
	bern_flag = 1, 
	filter_flag = 1, 
	neg_ent = 25,
	neg_rel = 0)

# dataloader for test
test_dataloader = TestDataLoader("./benchmarks/KGRC/", "link")

# define the model
transe = ComplEx(
	ent_tot = train_dataloader.get_ent_tot(),
	rel_tot = train_dataloader.get_rel_tot(),
	dim = 200 
#	p_norm = 1, 
#	norm_flag = True
)


# define the loss function
model = NegativeSampling(
	model = transe, 
	loss = SoftplusLoss(),
	batch_size = train_dataloader.get_batch_size(),
        regul_rate = 1.0
)

# train the model
trainer = Trainer(model = model, data_loader = train_dataloader, train_times = 1000, alpha = 0.5, opt_method = "adagrad", use_gpu = False)
trainer.run()
transe.save_checkpoint('./checkpoint/complEx_KGRC.ckpt')

# test the model
transe.load_checkpoint('./checkpoint/complEx_KGRC.ckpt')
tester = Tester(model = transe, data_loader = test_dataloader, use_gpu = False)
tester.run_link_prediction(type_constrain = True)
