package com.unla.SpringBootUnLa.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.unla.SpringBootUnLa.entities.Event;
import com.unla.SpringBootUnLa.entities.MedicionSensorAlumbrado;
import com.unla.SpringBootUnLa.entities.SensorAlumbradoInteligente;
import com.unla.SpringBootUnLa.helpers.ViewRouteHelper;
import com.unla.SpringBootUnLa.services.EventService;
import com.unla.SpringBootUnLa.services.MedicionSensorAlumbradoService;
import com.unla.SpringBootUnLa.services.SensorAlumbradoInteligenteService;

@Controller
@RequestMapping("/device/sensorAlumbradoInteligente")
public class MedicionSensorAlumbradoController {

	private final MedicionSensorAlumbradoService medicionService;

	@Autowired
	private SensorAlumbradoInteligenteService sensorService;
	private EventService eventService;

	public MedicionSensorAlumbradoController(MedicionSensorAlumbradoService medicionService,
			SensorAlumbradoInteligenteService sensorService, EventService eventService) {
		this.medicionService = medicionService;
		this.sensorService = sensorService;
		this.eventService = eventService;
	}

	// URL BASE
	@GetMapping("/lista/mediciones/{id}")
	public String listaMedicionesSensorAlumbrado(@PathVariable int id, Model model) {
		SensorAlumbradoInteligente device = sensorService.getSensorById(id);
		List<MedicionSensorAlumbrado> mediciones = medicionService.getMedicionesBySensor(device);
		model.addAttribute("sensorAlumbradoInteligente", device);
		model.addAttribute("medicionSensorAlumbrado", mediciones);
		return ViewRouteHelper.MEDICIONES_ALUMBRADO_INTELIGENTE;
	}

	// MAPEO PARA TOMAR LAS MEDICIONES Y GENERAR EVENTOS EN FUNCION
	@GetMapping("/lista/actualizar/{id}")
	public String actualizarEventosSensorAlumbrado(@PathVariable int id, Model model) {
		SensorAlumbradoInteligente sensor = sensorService.getSensorById(id); // Obtener el dispositivo por ID
		List<MedicionSensorAlumbrado> medicionesNoAnalizadas = medicionService
				.getMedicionesBySensorAndAnalizadaFalseOrderByFechaAsc(sensor);

		// Actualizar el estado y generar eventos para cada medición no analizada
		for (MedicionSensorAlumbrado medicion : medicionesNoAnalizadas) {
			sensor.setIntensidadLuz(medicion.getIntensidadLuz()); // Le asigno la ulitma medicion de luz al sensor

			if (!sensor.isEstado()) { // Si el estado actual del sensor es apagado
				if (medicion.getIntensidadLuz() <= sensor.getUmbralLuz()) { // Si la luz de la medicion cae por debajo o
																			// igual al umbral del sensor
					sensor.setEstado(true); // Cambiar el estado del sensor a prendido
					// Guardar el sensor actualizado en la base de datos
					sensorService.saveSensor(sensor);
					// Crear evento vinculado al sensor y con descripcion "Prender luz"
					Event eventoPrenderLuz = new Event(sensor, "Prender luz");
					// Guardar el evento creado en la base de datos
					eventService.saveEvent(eventoPrenderLuz);
				}
			} else { // Si el estado actual del sensor es prendido
				if (medicion.getIntensidadLuz() > sensor.getUmbralLuz()) {
					sensor.setEstado(false); // Cambiar el estado a apagado
					// Guardar el sensor actualizado en la base de datos
					sensorService.saveSensor(sensor);
					// Crear evento "Apagar luz"
					Event eventoApagarLuz = new Event(sensor, "Apagar luz");
					// Guardar el evento en la base de datos
					eventService.saveEvent(eventoApagarLuz);
				}
			}

			medicion.setAnalizada(true); // Marcar la medición como analizada
			medicionService.saveMedicion(medicion); // Actualizar la medición en la base de datos
		}

		model.addAttribute("sensorAlumbradoInteligente", sensor); // Pasarel sensor al modelo
		return ViewRouteHelper.LISTA_ALUMBRADO_INTELIGENTE;
	}

	@PostMapping
	public ResponseEntity<MedicionSensorAlumbrado> createMedicion(@RequestBody MedicionSensorAlumbrado medicion) {
		MedicionSensorAlumbrado createdMedicion = medicionService.saveMedicion(medicion);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdMedicion);
	}

	@GetMapping("/sensor/{sensorId}")
	public ResponseEntity<List<MedicionSensorAlumbrado>> getMedicionesBySensor(@PathVariable long sensorId) {
		SensorAlumbradoInteligente sensor = sensorService.getSensorById(sensorId);
		if (sensor == null) {
			return ResponseEntity.notFound().build();
		}
		List<MedicionSensorAlumbrado> mediciones = medicionService.getMedicionesBySensor(sensor);
		return ResponseEntity.ok(mediciones);
	}
}
