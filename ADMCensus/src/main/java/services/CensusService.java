package services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import repositories.CensusRepository;
import domain.Census;

@Service
@Transactional
public class CensusService {

	// Managed repository -----------------------------------------------------

	@Autowired
	private CensusRepository censusRepository;

	// Supporting services ----------------------------------------------------

	// Constructors -----------------------------------------------------------

	public CensusService() {
		super();
	}

	// Others Methods

	/**
	 *
	 * M�todo que crea un censo a partir de una votacion
	 *
	 * @param idVotacion
	 *            Identificador de la votacion
	 * @param username
	 *            Nombre de usuario que ha creado la votacion
	 * @param fecha_inicio
	 *            Fecha de inicio para la votacion
	 * @param fecha_fin
	 *            Fecha de fin para la votacion
	 * @param tituloVotacion
	 *            Cadena de texto con el titulo de la votacion
	 * @return census
	 * @throws ParseException
	 * 
	 */
	public Census create(int idVotacion, String username, String fecha_inicio, String fecha_fin, String tituloVotacion)
			throws ParseException {
		Assert.isTrue(!username.equals(""));
		Census c = new Census();
		long start_date = Long.parseLong(fecha_inicio);
		long finish_date = Long.parseLong(fecha_fin);

		Date fecha_comienzo = new Date(start_date);
		Date fecha_final = new Date(finish_date);

		Assert.isTrue(fecha_comienzo.before(fecha_final));

		c.setFechaFinVotacion(fecha_final);
		c.setFechaInicioVotacion(fecha_comienzo);

		c.setIdVotacion(idVotacion);
		c.setTituloVotacion(tituloVotacion);
		c.setUsername(username);
		HashMap<String, Boolean> vpo = new HashMap<String, Boolean>();
		c.setVoto_por_usuario(vpo);

		return c;
	}

	/**
	 *
	 * Metodo utilizado por cabina para actualizar el estado de voto de un
	 * usuario
	 * 
	 * @param idVotacion
	 *            Identificador de la votacion
	 * @param username
	 *            Nombre de usuario
	 * @return boolean
	 */
	public boolean updateUser(int idVotacion, String username) {
		boolean res = false;
		Assert.isTrue(!username.equals(""));
		Census c = findCensusByVote(idVotacion);
		HashMap<String, Boolean> vpo = c.getVoto_por_usuario();

		if (vpo.containsKey(username) && !vpo.get(username)) {

			vpo.remove(username);
			vpo.put(username, true);
			res = true;
		}

		c.setVoto_por_usuario(vpo);
		save(c);

		return res;
	}

	/**
	 *
	 * Metodo para devolver un json para saber si puede borrar o no puede borrar
	 * una votacion
	 * 
	 * @param idVotacion
	 *            Identificador de la votacion
	 * @param username
	 *            Nombre de usuario
	 * @return format json
	 */
	public String canDelete(int idVotacion, String username) {
		Assert.isTrue(!username.equals(""));
		String res = "";

		Census c = findCensusByVote(idVotacion);

		if (c.getVoto_por_usuario().isEmpty()) {
			res = "[{\"result\":\"yes\"}]";
			delete(c.getId(), username);
		} else {
			res = "[{\"result\":\"no\"}]";
		}

		return res;
	}

	/**
	 *
	 * Metodo para devolver un json para saber si un usuario puede votar en una
	 * determinada votacion
	 * 
	 * @param idVotacion
	 *            Identificador de la votacion
	 * @param username
	 *            Nombre de usuario
	 * @return string format json
	 */
	public String canVote(int idVotacion, String username) {
		Assert.isTrue(!username.equals(""));
		String res = "";

		Census c = findCensusByVote(idVotacion);

		if (c != null && c.getVoto_por_usuario().containsKey(username)) {
			if (!c.getVoto_por_usuario().get(username)) {
				res = "{\"result\":\"yes\"}";

			} else {

				res = "{\"result\":\"no\"}";
			}

		} else {
			res = "{\"result\":\"no\"}";
		}

		return res;
	}

	/**
	 *
	 * Metodo que devuelve todas las votaciones que un usuario no a votado y
	 * esta activa
	 *
	 * @param username
	 *            Nombre de usuario
	 * @return Collection<census>
	 */
	public Collection<Census> findCensusByUser(String username) {
		Assert.isTrue(!username.equals(""));
		Collection<Census> cs = new ArrayList<Census>();
		Collection<Census> aux = findAll(); // Obtengo todos los censos

		for (Census c : aux) {
			HashMap<String, Boolean> vpo = c.getVoto_por_usuario();
			if (vpo.containsKey(username)) {
				boolean votado = vpo.get(username);
				boolean activa = votacionActiva(c.getFechaInicioVotacion(), c.getFechaFinVotacion());
				// Si el usuario esta en el censo, la votaci�n esta activa y no
				// ha votado tengo que mostrar su censo.
				if (!votado && activa) {
					cs.add(c);
				}
			}

		}

		return cs;
	}

	/**
	 *
	 * Metodo que devuelve todas los censos que ha creado
	 *
	 * @param username
	 *            Nombre de usuario
	 * @return Collection<census>
	 */
	public Collection<Census> findCensusByCreator(String username) {
		Assert.isTrue(!username.equals(""));
		Collection<Census> cs = censusRepository.findCensusByCreator(username);
		return cs;
	}

	/**
	 * 
	 * A�ade un usuario con un username determidado a un censo
	 *
	 * @param censusId
	 *            Identificador del censo al que a�adir el usuario
	 * @param username
	 *            Creador (propietario) del censo
	 * @param username_add
	 *            Nombre de usuario que se va a a�adir al censo
	 */
	public void addUserToCensus(int censusId, String username, String username_add) {
		Census c = findOne(censusId);
		Assert.isTrue(votacionActiva(c.getFechaInicioVotacion(), c.getFechaFinVotacion()));
		Assert.isTrue(c.getUsername().equals(username));
		HashMap<String, Boolean> vpo = c.getVoto_por_usuario();
		Assert.isTrue(!vpo.containsKey(username_add));
		vpo.put(username_add, false);
		c.setVoto_por_usuario(vpo);
		save(c);
	}

	/**
	 * 
	 * Elimina un usuario con un username determidado a un censo, cumpliendo la
	 * condicion de que el usuario no tenga voto en ese censo
	 *
	 * @param censusId
	 *            Identificador del censo
	 * @param username
	 *            Creador (propietario) del censo
	 * @param username_add
	 *            Nombre de usuario que se va a a�adir al censo
	 */
	public void removeUserToCensus(int censusId, String username, String username_remove) {
		Census c = findOne(censusId);
		Assert.isTrue(votacionActiva(c.getFechaInicioVotacion(), c.getFechaFinVotacion()));
		HashMap<String, Boolean> vpo = c.getVoto_por_usuario();
		Assert.isTrue(c.getUsername().equals(username));
		Assert.isTrue(vpo.containsKey(username_remove) && !vpo.get(username_remove));
		vpo.remove(username_remove);
		c.setVoto_por_usuario(vpo);
		save(c);
	}

	/**
	 * 
	 * Persiste un censo Tambien actualizara este para a�adir usuarios y cambiar
	 * su valores de voto
	 * 
	 * @param census
	 * @return census
	 */
	public Census save(Census census) {
		Census c = censusRepository.save(census);
		return c;
	}

	/**
	 * 
	 * Borra un censo determinado cumpliendo la condicion que el mapa de votos
	 * por usuarios debe estar vacia
	 * 
	 * @param censusId
	 *            Identificador del censo
	 * @param username
	 */
	public void delete(int censusId, String username) {
		Census c = findOne(censusId);
		Assert.isTrue(c.getVoto_por_usuario().isEmpty());// Puedo borrarlo
															// siempre y cuando
															// no haya a�adido
															// usuario
		Assert.isTrue(c.getUsername().equals(username));
		censusRepository.delete(censusId);
	}

	/**
	 * 
	 * Encuentra un censo determidado por su id
	 * 
	 * @param censusId
	 *            Identificador del censo
	 * @return census
	 */
	public Census findOne(int censusId) {
		Census c = censusRepository.findOne(censusId);
		Assert.notNull(c);
		return c;
	}

	/**
	 * 
	 * Encuentra un censo por la votacion creada
	 * 
	 * @param idVotacion
	 *            Identificador de la votacion
	 * @return census
	 */
	public Census findOneByVote(int idVotacion) {
		Census c = censusRepository.findCensusByVote(idVotacion);
		Assert.notNull(c);
		return c;
	}

	/**
	 * 
	 * Metodo que devuelve un json informando sobre un determinado usuario y su
	 * estado en el voto
	 * 
	 * @param idVotacion
	 *            Identificador de la votacion
	 * @param username
	 *            Nombre de usuario a cerca del cual queremos obtener su estado
	 *            de voto
	 * @return String
	 */
	public String createResponseJson(int idVotacion, String username) {
		String response = "";
		Census c = findOneByVote(idVotacion);
		// formato: idVotacion, username, true/false
		if (c.getVoto_por_usuario().get(username)) {
			response = response + "{\"idVotacion\":" + idVotacion + ",\"username\":\"" + username + "\",\"result\":"
					+ c.getVoto_por_usuario().get(username) + "}";
		} else {
			response = response + "{\"result\":" + "user dont exist}";

		}
		return response;
	}

	/**
	 * 
	 * Encuentra todos los censos del sistema
	 * 
	 * @return Collection<Census>
	 */
	public Collection<Census> findAll() {
		return censusRepository.findAll();
	}

	/**
	 * 
	 * A�ade un usuario con un username determidado a un censo
	 *
	 * @param censusId
	 *            Identificador del censo al que a�adir el usuario
	 * @param username
	 *            Creador (propietario) del censo
	 * @param username_add
	 *            Nombre de usuario que se va a a�adir al censo
	 */
	public void addUserToCensu(int censusId, String username, String username_add) {
		Census c = findOne(censusId);
		Assert.isTrue(c.getUsername().equals(username));
		HashMap<String, Boolean> vpo = c.getVoto_por_usuario();
		Assert.isTrue(!vpo.get(username_add));
		vpo.put(username_add, false);

		c.setVoto_por_usuario(vpo);
		save(c);
	}

	/**
	 * 
	 * Elimina un usuario con un username determidado a un censo, cumpliendo la
	 * condicion de que el usuario no tenga voto en ese censo
	 *
	 * @param censusId
	 *            Identificador del censo
	 * @param username
	 *            Creador (propietario) del censo
	 * @param username_add
	 *            Nombre de usuario que se va a a�adir al censo
	 */
	public void removeUserToCensu(int censusId, String username, String username_remove) {
		Census c = findOne(censusId);
		Assert.isTrue(c.getUsername().equals(username));
		HashMap<String, Boolean> vpo = c.getVoto_por_usuario();
		Assert.isTrue(vpo.containsKey(username_remove));// contiene usuario
		Assert.isTrue(!vpo.get(username_remove));// Miro si ha votado
		vpo.remove(username_remove);
		c.setVoto_por_usuario(vpo);
		save(c);
	}

	/**
	 *
	 * Metodo para buscar un censo pur su votacion
	 *
	 * @param idVotacion
	 *            Identificador de la votacion sobre la que se va a buscar un
	 *            censo
	 * @return census
	 */
	public Census findCensusByVote(int idVotacion) {
		Census c = censusRepository.findCensusByVote(idVotacion);
		Assert.notNull(c);
		return c;
	}

	/**
	 *
	 * Metodo creado para saber si existe una votacion activa en el rango de
	 * fechas
	 * 
	 * @param fecha_inicio
	 *            Fecha inicio de la votacion
	 * @param fecha_fin
	 *            Fecha fin de la votacion
	 * @return true si esta activa
	 */
	private boolean votacionActiva(Date fecha_inicio, Date fecha_fin) {
		Boolean res = false;
		Date fecha_actual = new Date();
		Long fecha_actual_long = fecha_actual.getTime();
		Long fecha_inicio_long = fecha_inicio.getTime();
		Long fecha_fin_long = fecha_fin.getTime();
		if (fecha_inicio_long < fecha_actual_long && fecha_fin_long > fecha_actual_long) {
			res = true;
		}

		return res;
	}

}
