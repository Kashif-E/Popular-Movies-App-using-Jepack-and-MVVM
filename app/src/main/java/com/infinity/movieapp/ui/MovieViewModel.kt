package com.infinity.movieapp.ui

import android.app.Application
import androidx.lifecycle.*
import com.infinity.movieapp.MovieApplicationClass
import com.infinity.movieapp.models.databasemodels.ResultDatabaseModel
import com.infinity.movieapp.models.databasemodels.SavedResultDatabaseModel
import com.infinity.movieapp.models.databasemodels.asDomainModel
import com.infinity.movieapp.models.databasemodels.toDomainModel
import com.infinity.movieapp.models.domainmodel.Result
import com.infinity.movieapp.repository.MovieRepository
import com.infinity.movieapp.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch


class MovieViewModel(app: Application, private val movieRepository: MovieRepository) :
    AndroidViewModel(app) {


    private val latestMoviesMutable: MutableLiveData<Resource<List<Result>>> =
        MutableLiveData()
    val latestMovies: LiveData<Resource<List<Result>>>
        get() = latestMoviesMutable
    private val savedMoviesMutable: MutableLiveData<Resource<List<Result>>> =
        MutableLiveData()
    val savedMovies: LiveData<Resource<List<Result>>>
        get() = savedMoviesMutable
    private val popularMoviesMutable: MutableLiveData<Resource<List<Result>>> =
        MutableLiveData()
    val popularMovies: LiveData<Resource<List<Result>>>
        get() = popularMoviesMutable

    @FlowPreview
    var state = NetworkStatusTracker(app).networkStatus
        .map(
            onAvailable = { MyState.Fetched },
            onUnavailable = { MyState.Error }
        )
        .asLiveData(Dispatchers.IO)


    init {
        val isFirtTime =

            DataStoreManager(MovieApplicationClass.getInstance().baseContext).isFirstTimeFlow.asLiveData(
                Dispatchers.IO).value
        when (isFirtTime) {
            IsFirst.FIRST -> {
                viewModelScope.launch(Dispatchers.IO) {
                    movieRepository.refreshLatestMovieList()
                    movieRepository.refreshPopularMovieList()
                    DataStoreManager(MovieApplicationClass.getInstance().baseContext).setFirstTime(
                        IsFirst.NO)
                }
            }
        }/* viewModelScope.launch(Dispatchers.IO) {
            handleMoviesResponse(movieRepository.getLatestMovies(), latestMoviesMutable)
        }*/

        getpopularMovies()
        getLatestMovies()
        /* getPopularMoviesList()
         getLatestMoviesList()*/

    }

    private fun getLatestMovies() = viewModelScope.launch(Dispatchers.IO) {
            handleMoviesResponse(movieRepository.getPopularMovies().value!!, popularMoviesMutable)
        }



    private fun getpopularMovies() = viewModelScope.launch(Dispatchers.IO) {
        handleMoviesResponse(movieRepository.getLatestMovies().value!!, popularMoviesMutable)
    }




    private fun handleMoviesResponse(
        movies: List<ResultDatabaseModel>,
        list: MutableLiveData<Resource<List<Result>>>,
    ) {
        list.postValue(Resource.Loading())
        if (movies.asDomainModel().isNotEmpty()) {

            list.postValue(Resource.Success(movies.asDomainModel(),
                responseCode = 0))
        } else {
            list.postValue(Resource.Error("No Movies Found",
                responseCode = 0, data = emptyList()))
        }

    }


    fun saveMovie(movie: SavedResultDatabaseModel) = viewModelScope.launch {
        movieRepository.upsertSavedMoviesToDb(movie)
    }

    fun getSavedMovies() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedMovies = movieRepository.getSavedMovies()

            savedMoviesMutable.postValue(Resource.Success(savedMovies.toDomainModel() as MutableList<Result>,
                0))
        }
    }


    fun deleteMovie(movie: SavedResultDatabaseModel) = viewModelScope.launch(Dispatchers.IO) {
        movieRepository.deleteMovie(movie)
        movieRepository.getSavedMovies()
    }


}