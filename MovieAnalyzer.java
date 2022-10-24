import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {
    private final Supplier<Stream<movie>> movies;

    public MovieAnalyzer(String dataset_path) {
        movies = () -> {
            try {
                return Files.lines(Path.of(dataset_path)).skip(1).map(e -> {
                    String[] str = e.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (str.length == 16) {
                        return new movie(str[0], str[1], str[2], str[3], str[4], str[5], str[6], str[7], str[8], str[9], str[10], str[11], str[12], str[13], str[14], str[15]);
                    } else {
                        return new movie(str[0], str[1], str[2], str[3], str[4], str[5], str[6], str[7], str[8], str[9], str[10], str[11], str[12], str[13], str[14], null);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    //   Q1
    public Map<Integer, Integer> getMovieCountByYear() {
        TreeMap<Integer, Integer> map = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        movies.get().collect(Collectors.groupingBy(movie::getReleased_Year, Collectors.counting())).entrySet().forEach(e -> {
            map.put(e.getKey(), Integer.valueOf(Math.toIntExact(e.getValue())));
        });
        return map;
    }

    //    Q2
    public Map<String, Integer> getMovieCountByGenre() {
        Map<String, Integer> res = new TreeMap<>();
        movies.get().forEach(
                movie -> {
                    movie.getGenre().forEach(genre -> {
                        if (!res.containsKey(genre)) {
                            res.put(genre, 1);
                        } else {
                            int curr = res.get(genre);
                            res.put(genre, curr + 1);
                        }
                    });
                }
        );
        Comparator<Map.Entry<String, Integer>> e = new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        };
        List<Map.Entry<String, Integer>> list = res.entrySet()
                .stream()
                .sorted(e)
                .collect(Collectors.toList());
        Map<String, Integer> end = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return 1;
            }
        });
        list.forEach(stringIntegerEntry -> {
            end.put(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
        });
        return end;

    }

    //    Q3
    public Map<List<String>, Integer> getCoStarCount() {
        Map<List<String>, Integer> map = new HashMap<>();
        movies.get().forEach(e -> {
            String[] stars = new String[]{e.getStar1(), e.getStar2(), e.getStar3(), e.getStar4()};
            Arrays.sort(stars);
            List<List<String>> combinations = Arrays.asList(Arrays.asList(stars[0], stars[1]), Arrays.asList(stars[0], stars[2]), Arrays.asList(stars[0], stars[3]), Arrays.asList(stars[1], stars[2]), Arrays.asList(stars[1], stars[3]), Arrays.asList(stars[2], stars[3]));
            for (List<String> E : combinations) {
                if (!map.containsKey(E)) {
                    map.put(E, 1);
                } else {
                    int cur = map.get(E);
                    map.replace(E, ++cur);
                }
            }
        });
        List<Map.Entry<List<String>, Integer>> list = new ArrayList<Map.Entry<List<String>, Integer>>(map.entrySet());

        list.sort(new Comparator<Map.Entry<List<String>, Integer>>() {
            @Override
            public int compare(Map.Entry<List<String>, Integer> o1, Map.Entry<List<String>, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        Map<List<String>, Integer> res = new TreeMap<>(new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                return 1;
            }
        });

        list.forEach(e -> res.put(e.getKey(), e.getValue()));

        return res;
    }

    //    Q4
    public List<String> getTopMovies(int top_k, String by) {
        if (by.equals("runtime")) {
            return movies.get().sorted(Comparator.comparing(movie::getRuntime).reversed().thenComparing(movie::getSeries_Title)).map(movie::getSeries_Title).collect(Collectors.toList()).subList(0, top_k);
        } else {
            Comparator<movie> comparator = new Comparator<movie>() {
                @Override
                public int compare(movie o1, movie o2) {
                    return o2.getOverview().length() - o1.getOverview().length();
                }
            };
            return movies.get().sorted(comparator.thenComparing(movie::getSeries_Title)).map(movie::getSeries_Title).collect(Collectors.toList()).subList(0, top_k);
        }
    }

    //    Q5
    public List<String> getTopStars(int top_k, String by) {
        if (by.equals("rating")) {
            HashMap<String, Set<String>> map = new HashMap<>();
            movies.get().forEach(movie -> {
                String name = movie.getSeries_Title() + movie.getReleased_Year();
                List<String> stars = Arrays.asList(movie.getStar1(), movie.getStar2(), movie.getStar3(), movie.getStar4());
                stars.forEach(star -> {
                    if (!map.containsKey(star)) {
                        map.put(star, new HashSet<>());
                        map.get(star).add(name);
                    } else {
                        map.get(star).add(name);
                    }
                });
            });
            HashMap<String, Double> map2 = new HashMap<>();
            movies.get().forEach(movie -> {
                String name = movie.getSeries_Title() + movie.getReleased_Year();
                Double rate = Double.valueOf(movie.getIMDB_Rating());
                map2.put(name, rate);
            });
            HashMap<String, Double> avg = new HashMap<>();
            for (Map.Entry<String, Set<String>> star : map.entrySet()) {
                String person = star.getKey();
                Set<String> movie_list = star.getValue();
                Double sum = Double.valueOf(0);
                for (String movie : movie_list) {
                    sum += map2.get(movie);
                }
                avg.put(person, sum / movie_list.size());
            }
            Comparator<Map.Entry<String, Double>> comparator = new Comparator<Map.Entry<String, Double>>() {
                @Override
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    if ((o2.getValue() - o1.getValue()) < 0) return -1;
                    else if ((o2.getValue() - o1.getValue()) == 0) return 0;
                    else return 1;
                }
            };
            return avg.entrySet().stream().sorted(comparator.thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).collect(Collectors.toList()).subList(0, top_k);
        } else {
            Supplier<Stream<movie>> movieStream = () -> movies.get().filter(e -> (e.getGross() != null));
            HashMap<String, Set<String>> map = new HashMap<>();
            movieStream.get().forEach(movie -> {
                String name = movie.getSeries_Title() + movie.getReleased_Year();
                List<String> stars = Arrays.asList(movie.getStar1(), movie.getStar2(), movie.getStar3(), movie.getStar4());
                stars.forEach(star -> {
                    if (!map.containsKey(star)) {
                        map.put(star, new HashSet<>());
                        map.get(star).add(name);
                    } else {
                        map.get(star).add(name);
                    }
                });
            });
            HashMap<String, Long> map2 = new HashMap<>();
            movieStream.get().forEach(movie -> {
                String name = movie.getSeries_Title() + movie.getReleased_Year();
                Long gross = Long.valueOf(movie.getGross());
                map2.put(name, gross);
            });
            HashMap<String, Long> avg = new HashMap<>();
            for (Map.Entry<String, Set<String>> star : map.entrySet()) {
                String person = star.getKey();
                Set<String> movie_list = star.getValue();
                Long sum = 0L;
                for (String movie : movie_list) {
                    sum += map2.get(movie);
                }
                avg.put(person, sum / movie_list.size());
            }
            Comparator<Map.Entry<String, Long>> comparator = new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                    if ((o2.getValue() - o1.getValue()) < 0) return -1;
                    else if ((o2.getValue() - o1.getValue()) == 0) return 0;
                    else return 1;
                }
            };
            return avg.entrySet().stream().sorted(comparator.thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).collect(Collectors.toList()).subList(0, top_k);
        }
    }

    //    Q6
    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        return movies.get()
                .filter(e -> (e.getGenre().contains(genre)))
                .filter(e -> (e.getIMDB_Rating() >= min_rating))
                .filter(e -> (e.getRuntime() <= max_runtime))
                .sorted(Comparator.comparing(movie::getSeries_Title))
                .map(movie::getSeries_Title)
                .collect(Collectors.toList());
    }

    public static class movie {
        private final String Poster_Link;
        private final String Series_Title;
        private final Integer Released_Year;
        private final String Certificate;
        private final Integer Runtime;
        private final Set<String> Genre;
        private final Float IMDB_Rating;
        private final String Overview;
        private final Integer Meta_score;
        private final String Director;
        private final String Star1;
        private final String Star2;
        private final String Star3;
        private final String Star4;
        private final Integer No_of_Votes;
        private final Integer Gross;

        public movie(String poster_Link, String series_Title, String released_Year, String certificate, String runtime, String genre, String IMDB_Rating, String overview, String meta_score, String director, String star1, String star2, String star3, String star4, String no_of_Votes, String gross) {
            Poster_Link = poster_Link;
            Series_Title = series_Title.replace("\"", "");
            Released_Year = Integer.parseInt(released_Year);
            if (certificate != null && !certificate.equals("")) {
                Certificate = certificate;
            } else Certificate = null;
            Runtime = Integer.parseInt(runtime.trim().replace(" min", ""));
            Genre = new HashSet<>(Arrays.asList(genre.replace("\"", "").split(", ")));
            this.IMDB_Rating = Float.parseFloat(IMDB_Rating);
            if (overview.charAt(0) == '\"') {
                Overview = overview.substring(1, overview.length() - 1);
            } else {
                Overview = overview;
            }
            if (meta_score != null && !meta_score.equals("")) {
                Meta_score = Integer.parseInt(meta_score);
            } else Meta_score = null;
            Director = director;
            Star1 = star1;
            Star2 = star2;
            Star3 = star3;
            Star4 = star4;
            No_of_Votes = Integer.parseInt(no_of_Votes);
            if (gross != null) {
                Gross = Integer.parseInt(gross.replace(",", "").replace("\"", ""));
            } else Gross = null;
        }

        public String getPoster_Link() {
            return Poster_Link;
        }

        public String getSeries_Title() {
            return Series_Title;
        }

        public Integer getReleased_Year() {
            return Released_Year;
        }

        public String getCertificate() {
            return Certificate;
        }

        public Integer getRuntime() {
            return Runtime;
        }

        public Set<String> getGenre() {
            return Genre;
        }

        public Float getIMDB_Rating() {
            return IMDB_Rating;
        }

        public String getOverview() {
            return Overview;
        }

        public Integer getMeta_score() {
            return Meta_score;
        }

        public String getDirector() {
            return Director;
        }

        public String getStar1() {
            return Star1;
        }

        public String getStar2() {
            return Star2;
        }

        public String getStar3() {
            return Star3;
        }

        public String getStar4() {
            return Star4;
        }

        public Integer getNo_of_Votes() {
            return No_of_Votes;
        }

        public Integer getGross() {
            return Gross;
        }

    }
}