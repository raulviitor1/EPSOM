package br.poli.ecomp.pso.epso;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import br.poli.ecomp.pso.clpso.ParticleCLPSO;
import br.poli.ecomp.pso.fdrpso.ParticleFDRPSO;
import br.poli.ecomp.pso.hpsotvac.ParticleHSPSOTVAC;
import br.poli.ecomp.pso.lips.ParticleLIPS;
import br.poli.ecomp.pso.particle.Function;
import br.poli.ecomp.pso.particle.FunctionEnum;
import br.poli.ecomp.pso.particle.PSO;
import br.poli.ecomp.pso.particle.PSOResult;
import br.poli.ecomp.pso.particle.PSOType;
import br.poli.ecomp.pso.particle.Particle;
import br.poli.ecomp.pso.wpso.ParticleWPSO;

public class EPSO extends PSO {

	public static void main(String[] args) throws IOException {

		numParticles = 30;
		numIteration = 10000;
		numDimension = 10;
		numSimulation = 30;
		function = FunctionEnum.Sphere;

		File file;
		FileWriter fw = null;

		for (int simulation = 0; simulation < numSimulation; simulation ++) {

			int LP = 300;
			int k = 5;
			double[] pk = new double[k];
			PSOType selectedPSO;
			double[][] ns = new double[k][numIteration];
			double[][] nf = new double[k][numIteration];
			double[] sk = new double[k];

			for (int i = 0; i < pk.length; i++) {
				pk[i] = 1.0/k;
			}

			file = new File("EPSO" + "_" + function.name() + "_" + "simulation_" + (simulation + 1) + ".txt");
			fw = new FileWriter(file);
			fw.write("function;simulation;iteration;value\n");

			//Inicializa as part�culas
			Particle.particles = new ParticleWPSO[getNumParticles()];
			Particle.gBestParticle = null;
			Particle.gBestFitness = Double.MAX_VALUE;

			//Inicializa os outros algoritmos
			initialize();

			//Inicia randomicamente as particulas
			for (int i = 0; i < getNumParticles(); i++) {				
				Particle.particles[i] = new ParticleWPSO(numDimension, function, i);
				Particle.particles[i].calcularFitness(function);
			}
			
			for (int i = 0; i < getNumParticles(); i++) {				
				if (Particle.particles[i].getFitness() < Particle.gBestFitness) {
					Particle.gBestFitness = Particle.particles[i].getFitness();
					Particle.gBestParticle = Particle.particles[i];
				}
			}			

			//Iteracoes - Geracoes
			for (int iteration = 0; iteration < numIteration; iteration++) {

				double[] particleFitness = new double[PSO.getNumParticles()];
				for (int i = 0; i < PSO.getNumParticles(); i++) {
					particleFitness[i] = Particle.particles[i].getFitness();
				}

				//Escolhe o algoritmo da vez
				int selected_k = rouletteWheel(k, pk);
				selectedPSO = PSOType.values()[selected_k];

				//Altera o tipo de Particula
				Particle.changeParticlesType(selectedPSO);

				for (int i = 0; i < PSO.getNumParticles(); i++) {
					try {Particle.particles[i].updateVelocity(iteration);} catch (Exception e) {e.printStackTrace();}						
				}

				//Atualiza a posi��o
				for (int i = 0; i < PSO.getNumParticles(); i++) {
					Particle.particles[i].updatePosition();						
				}

				//Atualiza o fitness
				for (int i = 0; i < PSO.getNumParticles(); i++) {
					Particle.particles[i].calcularFitness(function);
				}

				for (int i = 0; i < PSO.getNumParticles(); i++) {
					if (Particle.particles[i].getFitness() < Particle.gBestFitness) {
						Particle.gBestFitness = Particle.particles[i].getFitness();
						Particle.gBestParticle = Particle.particles[i];
					}
					
					if (Particle.particles[i].getFitness() < particleFitness[i]) {
						ns[selected_k][iteration] += 1;
					} else {
						nf[selected_k][iteration] += 1;
					}
				}

				if (iteration > LP) {			
					for (int i = 0; i < k; i++) {
						double somaSucesso = 0.0;
						double somaFalha = 0.0;
						for (int j = iteration - LP - 1; j < iteration; j++) {
							somaSucesso += ns[i][j];
							somaFalha += nf[i][j];
						}

						sk[i] = (somaSucesso/(somaSucesso + somaFalha));
					}

					double somaSk = 0.001;

					for (int i = 0; i < k; i++) {
						somaSk += sk[i];
					}

					for (int i = 0; i < k; i++) {
						pk[i] = sk[i] / somaSk;
					}
				}

				//if (iteration % 50 == 0 || iteration == numIteration - 1) {
				//System.out.println("Melhor fitness: " + Particle.gBestFitness);
				fw.write(function.name() + ";" + (simulation + 1) + ";" + (iteration + 1) + ";" + Particle.gBestFitness + "\n");
				//}
			}//Fim itera�oes
			fw.close();
		}//Fim simula��es
	}

	private static void initialize() {
		ParticleWPSO.c1 = ParticleWPSO.c2 = 2;
		ParticleWPSO.wf = 1.5;
		ParticleWPSO.wi = 0.5;

		ParticleLIPS.nsize = 3;
		ParticleLIPS.c = 2;
		ParticleLIPS.X = 0.729;

		ParticleFDRPSO.wf = ParticleFDRPSO.w = 0.9;
		ParticleFDRPSO.wi = 0.2;
		ParticleFDRPSO.c1 = ParticleFDRPSO.c2 = 1;
		ParticleFDRPSO.c3 = 2;

		ParticleCLPSO.c1 = 2.25;
		ParticleCLPSO.wf = 0.9;
		ParticleCLPSO.wi = 0.2;

		ParticleHSPSOTVAC.c1f = ParticleHSPSOTVAC.c2 = 2.5;
		ParticleHSPSOTVAC.c1 = ParticleHSPSOTVAC.c2f = 0.5;
		ParticleHSPSOTVAC.v = 0.5;

	}

	private static int rouletteWheel (int k, double[] pk) {
		Random random = new Random();
		double r = random.nextDouble();
		double c = 0;
		for (int i = 0;i < k; i++) {
			c += pk[i];
			if (r <= c) {
				return i;
			}
		}
		return k - 1;
	}

	@Override
	public PSOResult call(int numParticles, int numIteration, int numDimension, int numSimulation,
			FunctionEnum function) throws Exception {
	
		PSO.numParticles = numParticles;
		PSO.numIteration = numIteration;
		PSO.numDimension = numDimension;
		PSO.numSimulation = numSimulation;
		PSO.function = function;
		
		PSOResult result = new PSOResult ();
		result.pso = "EPSO";
		
		StandardDeviation std_variation = new StandardDeviation();
		double[] std_variation_values = new double[numSimulation];
		DoubleSummaryStatistics average = new DoubleSummaryStatistics();
		
		//File file;
		//FileWriter fw = null;

		for (int simulation = 0; simulation < numSimulation; simulation ++) {
			
			File file;
			FileWriter fw = null;
			
			file = new File("EPSO" + "_" + function.name() + "_" + "simulation_" + (simulation + 1) + ".txt");
			fw = new FileWriter(file);
			fw.write("function;simulation;iteration;best_fitness\n");
			
			int LP = 500;
			int k = 5;
			double[] pk = new double[k];
			PSOType selectedPSO;
			double[][] ns = new double[k][numIteration];
			double[][] nf = new double[k][numIteration];
			double[] sk = new double[k];

			for (int i = 0; i < pk.length; i++) {
				pk[i] = 1.0/k;
			}

			//file = new File("EPSO" + "_" + function.name() + "_" + "simulation_" + (simulation + 1) + ".txt");
			//fw = new FileWriter(file);
			//fw.write("function;simulation;iteration;value\n");

			//Inicializa as part�culas
			Particle.particles = new ParticleWPSO[getNumParticles()];
			Particle.gBestParticle = null;
			Particle.gBestFitness = Double.MAX_VALUE;

			//Inicializa os outros algoritmos
			initialize();

			//Inicia randomicamente as particulas
			for (int i = 0; i < getNumParticles(); i++) {				
				Particle.particles[i] = new ParticleWPSO(numDimension, function, i);
				Particle.particles[i].calcularFitness(function);
			}
			
			for (int i = 0; i < getNumParticles(); i++) {				
				if (Particle.particles[i].getBestFitness() < Particle.gBestFitness) {
					Particle.gBestFitness = Particle.particles[i].getBestFitness();
					Particle.gBestParticle = Particle.particles[i];
				}
			}

			//Iteracoes - Geracoes
			for (int iteration = 0; iteration < numIteration; iteration++) {

				double[] particleFitness = new double[PSO.getNumParticles()];
				for (int i = 0; i < PSO.getNumParticles(); i++) {
					particleFitness[i] = Particle.particles[i].getFitness();
				}

				//Escolhe o algoritmo da vez
				int selected_k = rouletteWheel(k, pk);
				selectedPSO = PSOType.values()[selected_k];

				//Altera o tipo de Particula
				Particle.changeParticlesType(selectedPSO);

				for (int i = 0; i < PSO.getNumParticles(); i++) {
					try {Particle.particles[i].updateVelocity(iteration);} catch (Exception e) {e.printStackTrace();}						
				}

				//Atualiza a posi��o
				for (int i = 0; i < PSO.getNumParticles(); i++) {
					Particle.particles[i].updatePosition();						
				}

				//Atualiza o fitness
				for (int i = 0; i < PSO.getNumParticles(); i++) {
					Particle.particles[i].calcularFitness(function);
				}

				for (int i = 0; i < PSO.getNumParticles(); i++) {
					if (Particle.particles[i].getFitness() < Particle.gBestFitness) {
						Particle.gBestFitness = Particle.particles[i].getFitness();
						Particle.gBestParticle = Particle.particles[i];
					}
					
					if (Particle.particles[i].getFitness() < particleFitness[i]) {
						ns[selected_k][iteration] += 1;
					} else {
						nf[selected_k][iteration] += 1;
					}
				}

				if (iteration > LP) {
					for (int i = 0; i < k; i++) {
						double somaSucesso = 0;
						double somaFalha = 0;
						for (int j = iteration - LP - 1; j < iteration; j++) {
							somaSucesso += ns[i][j];
							somaFalha += nf[i][j];
						}

						sk[i] = (((double) somaSucesso)/(somaSucesso + somaFalha));
					}

					double somaSk = 0.001;

					for (int i = 0; i < k; i++) {
						somaSk += sk[i];
					}

					for (int i = 0; i < k; i++) {
						pk[i] = sk[i] / somaSk;
					}
				}

				if (iteration % 100 == 0 || iteration == numIteration - 1) {
					fw.write(function.name() + ";" + (simulation + 1) + ";" + (iteration + 1) + ";" + ParticleCLPSO.gBestFitness + "\n");
				}
				
				fw.close();	
			}//Fim itera�oes
			//fw.close();
			
			double diff = Particle.gBestFitness - Function.getFunctionBias();
			
			if (diff < result.best) {
				result.best = diff;
			}
			
			std_variation_values[simulation] = diff;
			average.accept(diff);
			
		}//Fim simula��es

		result.average = average.getAverage();
		result.std_variation = std_variation.evaluate(std_variation_values, result.average);		
		
		return result;
	}

}
